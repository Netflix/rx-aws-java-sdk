import scala.collection.JavaConversions._
import scala.util.matching.Regex

import java.lang.reflect._
import java.io.File
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets

import com.google.common.reflect._

case class ClientInfo(
  name: String,
  cinfo: ClassPath.ClassInfo,
  exceptionUnmarshallers: List[ClassPath.ClassInfo]
) {
  lazy val endpoint: Option[String] = AwsGenerate.endpoints.get(name.toLowerCase)
  lazy val className: String = s"Amazon${name}RxNettyClient"
  lazy val isStax: Boolean = {
    exceptionUnmarshallers.forall(_.load.getSuperclass.getSimpleName == "StandardErrorUnmarshaller")
  }
  lazy val isJson: Boolean = !isStax
}

object Pagination {
  val tokenGetter = """^(get(?:Next)?Token)$""".r
  val tokenSetter = """^(set(?:Next)?Token)$""".r

  def apply(result: Class[_], request: Class[_]): List[Pagination] = List(
    mkPagination(tokenGetter, result, tokenSetter, request)
  ).flatten

  def mkPagination(
    regexResult: Regex,
    classResult: Class[_],
    regexRequest: Regex,
    classRequest: Class[_]
  ): Option[Pagination] = {
    val getter = classResult.getDeclaredMethods.toList.find(_.getName match {
      case regexResult(n) => true
      case _ => false
    })
    val setter = classRequest.getDeclaredMethods.toList.find(_.getName match {
      case regexRequest(n) => true
      case _ => false
    })
    if (getter.isEmpty || setter.isEmpty) None
    //else if (getter.isEmpty || setter.isEmpty)
    //  throw new IllegalStateException(s"${getter} || ${setter}")
    else Some(
      Pagination(getter.get.getReturnType.getSimpleName, getter.get.getName, setter.get.getName)
    )
  }
}

case class Pagination(
  objType: String,
  getterName: String,
  setterName: String
)

object AwsGenerate {

  val clientPattern = """^Amazon([A-Za-z0-9]+?(?<!Async))?Client$""".r
  val exceptionUnmarshaller = """^(.*ExceptionUnmarshaller)$""".r

  val pageGetter = """^(get(?:Next)?Token)$""".r
  val pageSetter = """^(set(?:Next)?Token)$""".r

  def generate(dir: File): Seq[File] = {
    Files.createDirectories(dir.toPath)
    clientClasses.filter(_.endpoint.isDefined).map(c => {
      println(c.cinfo)
      val file = new File(dir, s"${c.className}.java")
      val content = mkContent(c)
      Files.write(file.toPath, content.getBytes(StandardCharsets.UTF_8))
      file
    })
  }

  def clientClasses: List[ClientInfo] = {
    val cl = classOf[com.amazonaws.services.ec2.AmazonEC2].getClassLoader
    val pkg = "com.amazonaws.services"
    ClassPath.from(cl).getTopLevelClassesRecursive(pkg).flatMap(cinfo => {
      cinfo.getSimpleName match {
        case clientPattern(prefix) => {
          val exceptionUnmarshallers = ClassPath.from(cl)
          .getTopLevelClassesRecursive(cinfo.getPackageName).flatMap(c2 => {
            c2.getSimpleName match {
              case exceptionUnmarshaller(n) => {
                Some(c2)
              }
              case _ => None
            }
          }).toList.sortBy(_.toString)
          Some(ClientInfo(prefix, cinfo, exceptionUnmarshallers))
        }
        case _ => None
      }
    }).toList.sortBy(_.toString)
  }

  def mkContent(c: ClientInfo): String = {
    mkHeader(c) + mkBody(c).mkString("\n") + mkFooter(c)
  }

  def mkHeader(c: ClientInfo): String = {
    val (imp, field, init) = mkExceptionUnmarshaller(c)
    headerTemplate
    .replaceAll("<<PKG>>", c.cinfo.getPackageName)
    .replaceAll("<<CLASSNAME>>", c.className)
    .replaceAll("<<ENDPOINT>>", c.endpoint.get)
    .replaceAll("<<EXCEPTION_UNMARSHALLER_IMPORT>>", imp)
    .replaceAll("<<EXCEPTION_UNMARSHALLER_FIELD>>", field)
    .replaceAll("<<EXCEPTION_UNMARSHALLER_INIT>>", init)
  }

  def mkBody(c: ClientInfo): Seq[String] = {
    val cls = c.cinfo.load
    cls.getDeclaredMethods
    .filter(_.getModifiers == 1)
    .filterNot(m => ignore.contains(m.getName))
    .filter(m => {
      (m.getParameterTypes.size == 0 && m.getReturnType.getSimpleName.endsWith("Result")) ||
      (m.getParameterTypes.size == 1 && m.getParameterTypes.head.getSimpleName.endsWith("Request"))
    })
    .groupBy(_.getName)
    .toList
    .filter(_._2.map(_.getParameterTypes.size).sum > 0)
    .sortBy(_._1)
//.filter(_.getName == "describeInstances")
//.filter(_.getName == "releaseAddress")
    .flatMap({ case (n, ms) => mkMethod(c, ms.toList) })
  }

  def mkMethod(c: ClientInfo, methods: List[Method]): Option[String] = {
    val method :: remainder = methods.sortBy(_.getParameterTypes.size).reverse
    val methodName = method.getName
    val requestType = method.getParameterTypes.head
    val strRequestType = requestType.getSimpleName
    val resultType = method.getReturnType
    val strResultType = resultType.getSimpleName match {
      case "void" => "Void"
      case s => s
    }

    val pagination = Pagination(resultType, requestType)

    val content = {
      if (pagination.size > 0)
        remainder.headOption.map(_ => paginatedTemplateNoArgs).getOrElse("") + paginatedTemplate
      else if (strResultType == "Void")
        remainder.headOption.map(_ => voidTemplateNoArgs).getOrElse("") + voidTemplate
      else
        remainder.headOption.map(_ => simpleTemplateNoArgs).getOrElse("") + simpleTemplate
    }

    Some(
      content
      .replaceAll("<<RESULT_TYPE>>", strResultType)
      .replaceAll("<<METHOD_NAME>>", methodName)
      .replaceAll("<<REQUEST_TYPE>>", strRequestType)
      .replaceAll("<<INIT_PAGINATION>>", mkInitPagination(pagination))
      .replaceAll("<<TOKEN_PARAMETERS>>", mkTokenParameters(pagination))
      .replaceAll("<<UPDATE_PAGINATION>>", mkUpdatePagination(pagination))
      .replaceAll("<<TYPE_UNMARSHALLER>>", { if (c.isStax) "Stax" else "Json" })
    )
  }

  def mkFooter(c: ClientInfo): String = footerTemplate

  def mkExceptionUnmarshaller(c: ClientInfo): (String, String, String) = {
    if (c.isStax) (
      "import org.w3c.dom.Node;",
      "protected List<Unmarshaller<AmazonServiceException,Node>> exceptionUnmarshallers;",
      (
        List(
          "exceptionUnmarshallers = new ArrayList<Unmarshaller<AmazonServiceException,Node>>();"
        ) ++
        c.exceptionUnmarshallers.map(c2 => {
          s"exceptionUnmarshallers.add(new ${c2.getSimpleName}());"
        }) ++
        List("exceptionUnmarshallers.add(new LegacyErrorUnmarshaller());")
      ).mkString("\n    ")
    )
    else (
      "",
      "protected List<JsonErrorUnmarshaller> exceptionUnmarshallers;",
      (
        List(
          "exceptionUnmarshallers = new ArrayList<JsonErrorUnmarshaller>();"
        ) ++
        c.exceptionUnmarshallers.map(c2 => {
          s"exceptionUnmarshallers.add(new ${c2.getSimpleName}());"
        }) ++
        List("exceptionUnmarshallers.add(new JsonErrorUnmarshaller());")
      ).mkString("\n    ")
    )
  }

  val ignore = List("dryRun", "getCachedResponseMetadata")

  val endpoints = Map(
    "autoscaling" -> "autoscaling.us-east-1.amazonaws.com",
    "cloudformation" -> "cloudformation.us-east-1.amazonaws.com",
    "cloudfront" -> "cloudfront.amazonaws.com",
    "cloudsearch" -> "cloudsearch.us-east-1.amazonaws.com",
    "cloudsearchdomain" -> "cloudsearchdomain.us-east-1.amazonaws.com",
    "cloudwatch" -> "monitoring.us-east-1.amazonaws.com",
    "codedeploy" -> "codedeploy.us-east-1.amazonaws.com",
    "cognitoidentity" -> "cognito-identity.us-east-1.amazonaws.com",
    "cognitosync" -> "cognito-sync.us-east-1.amazonaws.com",
    "config" -> "config.us-east-1.amazonaws.com",
    "directconnect" -> "directconnect.us-east-1.amazonaws.com",
    "dynamodb" -> "dynamodb.us-east-1.amazonaws.com",
    "ec2" -> "ec2.us-east-1.amazonaws.com",
    "ecs" -> "ecs.us-east-1.amazonaws.com",
    "elasticache" -> "elasticache.us-east-1.amazonaws.com",
    "elasticloadbalancing" -> "elasticloadbalancing.us-east-1.amazonaws.com",
    "elasticmapreduce" -> "elasticmapreduce.us-east-1.amazonaws.com",
    "elastictranscoder" -> "elastictranscoder.us-east-1.amazonaws.com",
    "glacier" -> "glacier.us-east-1.amazonaws.com",
    "identitymanagement" -> "iam.amazonaws.com",
    "importexport" -> "importexport.amazonaws.com",
    "kinesis" -> "kinesis.us-east-1.amazonaws.com",
    "rds" -> "rds.us-east-1.amazonaws.com",
    "redshift" -> "redshift.us-east-1.amazonaws.com",
    "route53" -> "route53.amazonaws.com",
    "route53domains" -> "route53domains.us-east-1.amazonaws.com",
    //"s3" -> "s3.amazonaws.com", // No unmarshaller for setting up the request
    //"s3encryption" -> "s3.amazonaws.com",
    //"simpledb" -> "sdb.amazonaws.com", // Legacy error handling
    "simpleemailservice" -> "email.us-east-1.amazonaws.com",
    "simpleworkflow" -> "swf.us-east-1.amazonaws.com",
    "sns" -> "sns.us-east-1.amazonaws.com",
    "sqs" -> "sqs.us-east-1.amazonaws.com"
  )

  val headerTemplate = """
package <<PKG>>;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

<<EXCEPTION_UNMARSHALLER_IMPORT>>

import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.handlers.*;
import com.amazonaws.http.*;
import com.amazonaws.internal.*;
import com.amazonaws.metrics.*;
import com.amazonaws.regions.*;
import com.amazonaws.transform.*;
import com.amazonaws.util.*;
import com.amazonaws.util.AWSRequestMetrics.Field;

import com.amazonaws.services.*;
import <<PKG>>.model.*;
import <<PKG>>.model.transform.*;

import rx.Observable;

public class <<CLASSNAME>> extends AmazonRxNettyHttpClient {

  public <<CLASSNAME>>() {
    super();
  }

  public <<CLASSNAME>>(AWSCredentialsProvider credProvider) {
    super(credProvider);
  }

  public <<CLASSNAME>>(ClientConfiguration config) {
    super(config);
  }

  public <<CLASSNAME>>(AWSCredentialsProvider credProvider, ClientConfiguration config) {
    super(credProvider, config);
  }

  <<EXCEPTION_UNMARSHALLER_FIELD>>

  @Override
  protected void init() {
    this.setEndpoint("<<ENDPOINT>>");
    <<EXCEPTION_UNMARSHALLER_INIT>>
  }
"""

  def mkInitPagination(pagination: List[Pagination]): String = {
    pagination.map(p => s"    request.${p.setterName}(null);").mkString("\n")
  }
  def mkTokenParameters(pagination: List[Pagination]): String = {
    pagination.map(p => s"request.${p.getterName}()").mkString(", ")
  }
  def mkUpdatePagination(pagination: List[Pagination]): String = {
    pagination.map(p => s"        request.${p.setterName}(result.${p.getterName}());").mkString("\n")
  }

  val paginatedTemplateNoArgs = """
  public Observable<PaginatedServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>() {
    return <<METHOD_NAME>>(new <<REQUEST_TYPE>>());
  }
"""

  val paginatedTemplate = """
  public Observable<PaginatedServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>(
    final <<REQUEST_TYPE>> request
  ) {
    final AtomicInteger cntRef = new AtomicInteger(0);
    <<INIT_PAGINATION>>
    return Observable.using(
      () -> { return null; },
      (create) -> {
        long startTime = System.currentTimeMillis();
        String token = mkToken(<<TOKEN_PARAMETERS>>);
        if (token == null && cntRef.get() > 0) return Observable.just(null);
        else {
          ExecutionContext executionContext = createExecutionContext(request);
          AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
          Request<<<REQUEST_TYPE>>> mReq = new <<REQUEST_TYPE>>Marshaller().marshall(request);
          mReq.setAWSRequestMetrics(awsRequestMetrics);
          <<RESULT_TYPE>><<TYPE_UNMARSHALLER>>Unmarshaller unmarshaller = <<RESULT_TYPE>><<TYPE_UNMARSHALLER>>Unmarshaller.getInstance();
          return invoke<<TYPE_UNMARSHALLER>>(mReq, unmarshaller, exceptionUnmarshallers, executionContext)
          .doOnNext(result -> {
            <<UPDATE_PAGINATION>>
          })
          .map(result -> {
            return new PaginatedServiceResult<<<RESULT_TYPE>>>(startTime, token, result);
          });
        }
      },
      (dispose) -> {
        cntRef.incrementAndGet();
      }
    )
    .repeat()
    .takeWhile(result -> {
      return result != null;
    });
  }
"""

  val simpleTemplateNoArgs = """
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>() {
    return <<METHOD_NAME>>(new <<REQUEST_TYPE>>());
  }
"""

  val simpleTemplate = """
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>(
    final <<REQUEST_TYPE>> request
  ) {
    return Observable.just(request)
    .flatMap(r -> {
      long startTime = System.currentTimeMillis();
      ExecutionContext executionContext = createExecutionContext(request);
      AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
      return Observable.just(0).map(i -> {
        return new <<REQUEST_TYPE>>Marshaller().marshall(r);
      })
      .flatMap(mReq -> {
        mReq.setAWSRequestMetrics(awsRequestMetrics);
        <<RESULT_TYPE>><<TYPE_UNMARSHALLER>>Unmarshaller unmarshaller = <<RESULT_TYPE>><<TYPE_UNMARSHALLER>>Unmarshaller.getInstance();
        return invoke<<TYPE_UNMARSHALLER>>(mReq, unmarshaller, exceptionUnmarshallers, executionContext);
      })
      .map(result -> {
        return new ServiceResult<<<RESULT_TYPE>>>(startTime, result);
      });
    });
  }
"""

  val voidTemplateNoArgs = """
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>()
    return <<METHOD_NAME>>(new <<REQUEST_TYPE>>());
  }
"""

  val voidTemplate = """
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>(
    final <<REQUEST_TYPE>> request
  ) {
    return Observable.just(request)
    .flatMap(r -> {
      long startTime = System.currentTimeMillis();
      ExecutionContext executionContext = createExecutionContext(request);
      AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
      return Observable.just(0).map(i -> {
        return new <<REQUEST_TYPE>>Marshaller().marshall(r);
      })
      .flatMap(mReq -> {
        mReq.setAWSRequestMetrics(awsRequestMetrics);
        Unmarshaller<Void,<<TYPE_UNMARSHALLER>>UnmarshallerContext> unmarshaller = (Unmarshaller<Void,<<TYPE_UNMARSHALLER>>UnmarshallerContext>) null;
        return invoke<<TYPE_UNMARSHALLER>>(mReq, unmarshaller, exceptionUnmarshallers, executionContext);
      })
      .map(result -> {
        return new ServiceResult<<<RESULT_TYPE>>>(startTime, result);
      });
    });
  }
"""

  val footerTemplate = """
}
"""
}
