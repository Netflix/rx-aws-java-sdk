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
  lazy val interfaceName: String = s"Amazon${name}RxNetty"
  lazy val isStax: Boolean = {
    exceptionUnmarshallers.forall(_.load.getSuperclass.getSimpleName == "StandardErrorUnmarshaller")
  }
  lazy val isJson: Boolean = !isStax
}

object Pagination {
  val tokenRegex             = """^(get|set)(?:Next)?Token$""".r
  val keyRegex               = """^(get|set)(?:LastEvaluated|ExclusiveStart)Key$""".r
  val markerRegex            = """^(get|set)(?:Next)?Marker$""".r
  val recordNameRegex        = """^(get|set)(?:Start|Next)?RecordName$""".r
  val recordTypeRegex        = """^(get|set)(?:Start|Next)?RecordType$""".r
  val recordIdentifierRegex  = """^(get|set)(?:Start|Next)?RecordIdentifier$""".r


  def apply(request: Class[_], result: Class[_]): List[Pagination] = List(
    mkPagination(tokenRegex, request, result),
    mkPagination(keyRegex, request, result),
    mkPagination(markerRegex, request, result),
    mkPagination(recordNameRegex, request, result),
    mkPagination(recordTypeRegex, request, result),
    mkPagination(recordIdentifierRegex, request, result)
  ).flatten

  def mkPagination(
    regex: Regex,
    request: Class[_],
    result: Class[_]
  ): Option[Pagination] = {
    val requestGetter = request.getDeclaredMethods.toList.filter(_.getName match {
      case regex("get") => true
      case _ => false
    }).filter(_.getParameterTypes.size == 0)
    val requestSetter = request.getDeclaredMethods.toList.filter(_.getName match {
      case regex("set") => true
      case _ => false
    }).filter(_.getParameterTypes.size == 1)
    val resultGetter = result.getDeclaredMethods.toList.filter(_.getName match {
      case regex("get") => true
      case _ => false
    }).filter(_.getParameterTypes.size == 0)
    if (requestGetter.isEmpty || requestSetter.isEmpty || resultGetter.isEmpty) None
    //else if (getter.isEmpty || setter.isEmpty)
    //  throw new IllegalStateException(s"${getter} || ${setter}")
    else Some(
      Pagination(
        requestGetter.head.getReturnType.getSimpleName,
        requestGetter.head.getName,
        requestSetter.head.getName,
        resultGetter.head.getName,
        requestSetter.size > 1
      )
    )
  }
}

case class Pagination(
  objType: String,
  requestGetterName: String,
  requestSetterName: String,
  resultGetterName: String,
  multipleSetters: Boolean
) {
  def strCast: String = {
    if (multipleSetters) s"(${objType})"
    else ""
  }
}

object AwsGenerate {

  val clientPattern = """^Amazon([A-Za-z0-9]+?(?<!Async))?Client$""".r
  val exceptionUnmarshaller = """^(.*ExceptionUnmarshaller)$""".r

  val pageGetter = """^(get(?:Next)?Token)$""".r
  val pageSetter = """^(set(?:Next)?Token)$""".r

  def generate(dir: File, pkgSuffixes: List[String]): Seq[File] = {
    Files.createDirectories(dir.toPath)
    clientClasses(pkgSuffixes).filter(_.endpoint.isDefined).flatMap(c => {
      println(c.cinfo)
      val iFile = new File(dir, s"${c.interfaceName}.java")
      val file = new File(dir, s"${c.className}.java")
      val (iContent, content) = mkContent(c)
      Files.write(iFile.toPath, iContent.getBytes(StandardCharsets.UTF_8))
      Files.write(file.toPath, content.getBytes(StandardCharsets.UTF_8))
      List(iFile, file)
    })
  }

  def clientClasses(pkgSuffixes: List[String]): List[ClientInfo] = {
    val cl = classOf[com.amazonaws.services.ec2.AmazonEC2].getClassLoader
    pkgSuffixes.flatMap(pkgSuffix => {
      val pkg = s"com.amazonaws.services.${pkgSuffix}"
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
      })
    }).toList.sortBy(_.toString)
  }
  def mkContent(c: ClientInfo): (String, String) = (
    mkIHeader(c) + mkBody(c, true).mkString("\n") + mkIExtraMethods(c) + mkIFooter(c),
    mkHeader(c) + mkBody(c).mkString("\n") + mkExtraMethods(c) + mkFooter(c)
  )

  def mkIHeader(c: ClientInfo): String = {
    iHeaderTemplate
    .replaceAll("<<PKG>>", c.cinfo.getPackageName)
    .replaceAll("<<IFACENAME>>", c.interfaceName)
  }

  def mkHeader(c: ClientInfo, isInterface: Boolean = false): String = {
    val (imp, field, init) = mkExceptionUnmarshaller(c)
    headerTemplate
    .replaceAll("<<PKG>>", c.cinfo.getPackageName)
    .replaceAll("<<CLASSNAME>>", c.className)
    .replaceAll("<<IFACENAME>>", c.interfaceName)
    .replaceAll("<<ENDPOINT>>", c.endpoint.get)
    .replaceAll("<<EXCEPTION_UNMARSHALLER_IMPORT>>", imp)
    .replaceAll("<<EXCEPTION_UNMARSHALLER_FIELD>>", field)
    .replaceAll("<<EXCEPTION_UNMARSHALLER_INIT>>", init)
  }

  def mkBody(c: ClientInfo, isInterface: Boolean = false): Seq[String] = {
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
    .flatMap({ case (n, ms) => mkMethod(c, ms.toList, isInterface) })
  }

  def mkMethod(c: ClientInfo, methods: List[Method], isInterface: Boolean): Option[String] = {
    val method :: remainder = methods.sortBy(_.getParameterTypes.size).reverse
    val methodName = method.getName
    val requestType = method.getParameterTypes.head
    val strRequestType = requestType.getSimpleName
    val resultType = method.getReturnType
    val strResultType = resultType.getSimpleName match {
      case "void" => "Void"
      case s => s
    }

    val pagination = Pagination(requestType, resultType)

    val content = {
      if (pagination.size > 0) {
        if (isInterface)
          remainder.headOption.map(_ => paginatedITemplateNoArgs).getOrElse("") + paginatedITemplate
         else
          remainder.headOption.map(_ => paginatedTemplateNoArgs).getOrElse("") + paginatedTemplate
      }
      else if (strResultType == "Void") {
        if (isInterface)
          remainder.headOption.map(_ => voidITemplateNoArgs).getOrElse("") + voidITemplate
        else
          remainder.headOption.map(_ => voidTemplateNoArgs).getOrElse("") + voidTemplate
      }
      else {
        if (isInterface)
          remainder.headOption.map(_ => simpleITemplateNoArgs).getOrElse("") + simpleITemplate
        else
          remainder.headOption.map(_ => simpleTemplateNoArgs).getOrElse("") + simpleTemplate
      }
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

  def mkIFooter(c: ClientInfo): String = iFooterTemplate
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

  val iHeaderTemplate = """
package <<PKG>>;

import com.amazonaws.services.*;
import <<PKG>>.model.*;
import rx.Observable;

public interface <<IFACENAME>> {

  public void setEndpoint(String endpoint);
"""

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

public class <<CLASSNAME>> extends AmazonRxNettyHttpClient implements <<IFACENAME>> {

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
    setEndpoint("<<ENDPOINT>>");
    <<EXCEPTION_UNMARSHALLER_INIT>>
  }
"""

  def mkInitPagination(pagination: List[Pagination]): String = {
    pagination.map(p => s"request.${p.requestSetterName}(${p.strCast}null);").mkString("\n    ")
  }
  def mkTokenParameters(pagination: List[Pagination]): String = {
    pagination.map(p => s"((request.${p.requestGetterName}() == null) ? null : request.${p.requestGetterName}().toString())").mkString(", ")
  }
  def mkUpdatePagination(pagination: List[Pagination]): String = {
    pagination.map(p => s"        request.${p.requestSetterName}(result.${p.resultGetterName}());").mkString("\n")
  }

  val paginatedITemplateNoArgs = """
  public Observable<PaginatedServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>();
"""

  val paginatedITemplate = """
  public Observable<PaginatedServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>(
    <<REQUEST_TYPE>> request
  );
"""

  val paginatedTemplateNoArgs = """
  @Override
  public Observable<PaginatedServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>() {
    return <<METHOD_NAME>>(new <<REQUEST_TYPE>>());
  }
"""

  val paginatedTemplate = """
  @Override
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
        return Observable.just(request)
        .observeOn(RxSchedulers.computation())
        .flatMap(r -> {
          if (token == null && cntRef.get() > 0) return Observable.just(null);
          else {
            ExecutionContext executionContext = createExecutionContext(r);
            AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
            Request<<<REQUEST_TYPE>>> mReq = new <<REQUEST_TYPE>>Marshaller().marshall(r);
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
        });
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

  val simpleITemplateNoArgs = """
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>();
"""

  val simpleITemplate = """
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>(
    <<REQUEST_TYPE>> request
  );
"""

  val simpleTemplateNoArgs = """
  @Override
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>() {
    return <<METHOD_NAME>>(new <<REQUEST_TYPE>>());
  }
"""

  val simpleTemplate = """
  @Override
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>(
    final <<REQUEST_TYPE>> request
  ) {
    return Observable.just(request)
    .observeOn(RxSchedulers.computation())
    .flatMap(r -> {
      long startTime = System.currentTimeMillis();
      ExecutionContext executionContext = createExecutionContext(r);
      AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
      Request<<<REQUEST_TYPE>>> mReq = new <<REQUEST_TYPE>>Marshaller().marshall(r);
      mReq.setAWSRequestMetrics(awsRequestMetrics);
      <<RESULT_TYPE>><<TYPE_UNMARSHALLER>>Unmarshaller unmarshaller = <<RESULT_TYPE>><<TYPE_UNMARSHALLER>>Unmarshaller.getInstance();
      return invoke<<TYPE_UNMARSHALLER>>(mReq, unmarshaller, exceptionUnmarshallers, executionContext)
      .map(result -> {
        return new ServiceResult<<<RESULT_TYPE>>>(startTime, result);
      });
    });
  }
"""

  val voidITemplateNoArgs = """
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>();
"""

  val voidITemplate = """
  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>(
    <<REQUEST_TYPE>> request
  );
"""

  val voidTemplateNoArgs = """
    |  @Override
    |  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>()
    |    return <<METHOD_NAME>>(new <<REQUEST_TYPE>>());
    |  }
    |""".stripMargin

  val voidTemplate = """
    |  @Override
    |  public Observable<ServiceResult<<<RESULT_TYPE>>>> <<METHOD_NAME>>(
    |    final <<REQUEST_TYPE>> request
    |  ) {
    |    return Observable.just(request)
    |    .observeOn(RxSchedulers.computation())
    |    .flatMap(r -> {
    |      long startTime = System.currentTimeMillis();
    |      ExecutionContext executionContext = createExecutionContext(r);
    |      AWSRequestMetrics awsRequestMetrics = executionContext.getAwsRequestMetrics();
    |      Request<<<REQUEST_TYPE>>> mReq = new <<REQUEST_TYPE>>Marshaller().marshall(r);
    |      mReq.setAWSRequestMetrics(awsRequestMetrics);
    |      Unmarshaller<Void,<<TYPE_UNMARSHALLER>>UnmarshallerContext> unmarshaller = (Unmarshaller<Void,<<TYPE_UNMARSHALLER>>UnmarshallerContext>) null;
    |      return invoke<<TYPE_UNMARSHALLER>>(mReq, unmarshaller, exceptionUnmarshallers, executionContext)
    |      .map(result -> {
    |        return new ServiceResult<<<RESULT_TYPE>>>(startTime, result);
    |      });
    |    });
    |  }
    |""".stripMargin

  val iFooterTemplate = """
    |}
    |""".stripMargin

  val footerTemplate = """
    |}
    |""".stripMargin

  def mkIExtraMethods(c: ClientInfo): String = {
    extraMethods.get(c.name.toLowerCase).map(_.head).getOrElse("")
  }

  def mkExtraMethods(c: ClientInfo): String = {
    extraMethods.get(c.name.toLowerCase).map(_.last).getOrElse("")
  }

  val extraMethods = Map(
    "elasticloadbalancing" -> List(
      """
      |  public Observable<NamedServiceResult<DescribeInstanceHealthResult>> describeInstanceHealth();
      |
      |  public Observable<NamedServiceResult<DescribeLoadBalancerAttributesResult>> describeLoadBalancerAttributes();
      |""".stripMargin,
      """
      |  public Observable<NamedServiceResult<DescribeInstanceHealthResult>> describeInstanceHealth() {
      |    return describeLoadBalancers().flatMap(r0 -> {
      |      return Observable.from(r0.result.getLoadBalancerDescriptions());
      |    }).flatMap(elb -> {
      |      String elbName = elb.getLoadBalancerName();
      |      return describeInstanceHealth(
      |        new DescribeInstanceHealthRequest().withLoadBalancerName(elbName)
      |      ).map(r1 -> {
      |        return new NamedServiceResult<DescribeInstanceHealthResult>(r1.startTime, elbName, r1.result);
      |      });
      |    });
      |  }
      |
      |  public Observable<NamedServiceResult<DescribeLoadBalancerAttributesResult>> describeLoadBalancerAttributes() {
      |    return describeLoadBalancers().flatMap(r0 -> {
      |      return Observable.from(r0.result.getLoadBalancerDescriptions());
      |    }).flatMap(elb -> {
      |      String elbName = elb.getLoadBalancerName();
      |      return describeLoadBalancerAttributes(
      |        new DescribeLoadBalancerAttributesRequest().withLoadBalancerName(elbName)
      |      ).map(r1 -> {
      |        return new NamedServiceResult<DescribeLoadBalancerAttributesResult>(r1.startTime, elbName, r1.result);
      |      });
      |    });
      |  }
      |""".stripMargin
    ),
    "route53" -> List(
      """
      |  public Observable<PaginatedServiceResult<ListResourceRecordSetsResult>> listResourceRecordSets();
      |""".stripMargin,
      """
      |  public Observable<PaginatedServiceResult<ListResourceRecordSetsResult>> listResourceRecordSets() {
      |    return listHostedZones().reduce(new ArrayList<String>(), (acc, r) -> {
      |      r.result.getHostedZones().stream().forEach(h -> acc.add(h.getId()));
      |      return acc;
      |    })
      |    .flatMap(list -> Observable.from(list))
      |    .flatMap(id -> {
      |      return listResourceRecordSets(new ListResourceRecordSetsRequest().withHostedZoneId(id));
      |    });
      |}
      |""".stripMargin
    )
  )
}
