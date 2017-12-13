### Producing a SOAP web service with Spring Boot

1. You don't need a webapp project, a maven quickstart project is sufficient

2. maven pom

   ```xml
   	<parent>
   		<groupId>org.springframework.boot</groupId>
   		<artifactId>spring-boot-starter-parent</artifactId>
   		<version>1.5.9.RELEASE</version>
   	</parent>
   	<dependencies>
   		<dependency>
   			<groupId>org.springframework.boot</groupId>
   			<artifactId>spring-boot-starter</artifactId>
   			<version>1.5.9.RELEASE</version>
   		</dependency>
   		<dependency>
   			<groupId>org.springframework.boot</groupId>
   			<artifactId>spring-boot-starter-web-services</artifactId>
   		</dependency>
   		<dependency>
   			<groupId>wsdl4j</groupId>
   			<artifactId>wsdl4j</artifactId>
   		</dependency>
   	</dependencies>
   	<build>
   		<plugins>
   			<plugin>
   				<groupId>org.springframework.boot</groupId>
   				<artifactId>spring-boot-maven-plugin</artifactId>
   			</plugin>
   			<plugin>
   				<groupId>org.codehaus.mojo</groupId>
   				<artifactId>jaxb2-maven-plugin</artifactId>
   				<version>2.3.1</version>
   				<executions>
   					<execution>
   						<id>xjc</id>
   						<goals><goal>xjc</goal></goals>
   					</execution>
   				</executions>
   				<configuration>
   <!-- The package of where wanted the generated code to be in -->
   <!-- Generated classes are placed in target/generated-sources/jaxb/ directory -->
   <!-- if there are already classes with the same name inside the same packages in main, "mvn package" will cause "Type ... is already defined" -->
   					<packageName>hello.artifacts</packageName>
   					<encoding>UTF-8</encoding>
                         <sourceType>xmlschema</sourceType>
   					<sources>
   						<source>src/main/resources/countries.xsd</source>
   					</sources>
   				</configuration>
   			</plugin>
   		</plugins>
   	</build>
   </project>
   ```

3. Create an XML schema to define the domain

   1. if you forgot what is XML schema, review [here](https://www.w3schools.com/xml/schema_intro.asp) at W3CSchool
   2. the path is`src/main/resources/countries.xsd`, which matches the <source> in pom.xml
   3. There is a sample in the Spring guide so I will not post it here

4. Create endpoint

   ```java
   @Endpoint
   public class CountryEndpoint {
   	private static final String NAMESPACE_URI = "http://raidencentral.com/countries";
     	@Autowired
   	private CountryRepository countryRepository;

   	@PayloadRoot(namespace = NAMESPACE_URI, localPart = "getCountryRequest")
   	@ResponsePayload
   	public GetCountryResponse getCountry(@RequestPayload GetCountryRequest request) {
   		GetCountryResponse response = new GetCountryResponse();
   		response.setCountry(countryRepository.findCountry(request.getName()));
   		return response;
   	}
   }
   ```

   1. the NAMESPACE_URL matches the namespace inside your xml schema(.xsd file)
   2. @Endpoint register the class as a endpoint to process incoming SOAP messages, it is a specialization of @Component, so it will be picked up when scanned
   3. @PayloadRoot 
      1. marks an endpoint method as the handler for an incoming request. The annotation 
         values signify the request payload root element that is handled by the method.
      2. pick the handler method based on the message’s **namespace** and **localPart**.
   4. @RequestPayload Annotation 
      1. indicates that a method parameter should be bound to the [request payload]. Supported for annotated endpoint methods.
      2. indicates that the incoming message will be mapped to the method’s `request` parameter.
   5. The [`@ResponsePayload`](https://docs.spring.io/spring-ws/sites/2.0/apidocs/org/springframework/ws/server/endpoint/annotation/ResponsePayload.html) annotation makes Spring WS map the returned value to the response payload.

5. Configure web service beans

   ```java
   @EnableWs
   @Configuration
   public class WebServiceConfig extends WsConfigurerAdapter {
   	@Bean
       //messageDispatcherServlet is used to handle SOAP messages
     //By naming this bean messageDispatcherServlet, it does not replace Spring Boot’s default DispatcherServlet bean.
   	public ServletRegistrationBean messageDispatcherServlet(ApplicationContext applicationContext) {
   		MessageDispatcherServlet servlet = new MessageDispatcherServlet();
   		servlet.setApplicationContext(applicationContext);
   		servlet.setTransformWsdlLocations(true);
   		return new ServletRegistrationBean(servlet, "/ws/*");
   	}
   	@Bean(name = "countries")
   	public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema countriesSchema) {
   		DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
   		wsdl11Definition.setPortTypeName("CountriesPort");
   		wsdl11Definition.setLocationUri("/ws");
   		wsdl11Definition.setTargetNamespace("http://raidencentral.com/countries");
   		wsdl11Definition.setSchema(countriesSchema);
   		return wsdl11Definition;
   	}
   	@Bean
   	public XsdSchema countriesSchema() {
   		return new SimpleXsdSchema(new ClassPathResource("countries.xsd"));
   	}
   ```

   1. By naming this bean messageDispatcherServlet, it does **not** replace Spring Boot’s default DispatcherServlet bean.
   2. [`DefaultWsdl11Definition`](https://docs.spring.io/spring-ws/sites/2.0/apidocs/org/springframework/ws/wsdl/wsdl11/DefaultWsdl11Definition.html) exposes a standard WSDL 1.1 using [`XsdSchema`](https://docs.spring.io/spring-ws/sites/2.0/apidocs/org/springframework/xml/xsd/XsdSchema.html)
   3. It’s important to notice that you need to specify bean names for [`MessageDispatcherServlet`](https://docs.spring.io/spring-ws/sites/2.0/apidocs/org/springframework/ws/transport/http/MessageDispatcherServlet.html) and [`DefaultWsdl11Definition`](https://docs.spring.io/spring-ws/sites/2.0/apidocs/org/springframework/ws/wsdl/wsdl11/DefaultWsdl11Definition.html). Bean names determine the URL under which web service and the generated WSDL file is available. In this case, the WSDL will be available under `http://<host>:<port>/ws/countries.wsdl`.
      1. `/ws` is set by `wsdl11Definition.setLocationUri("/ws");`
      2. `countries.wsdl` is set by `@Bean(name = "countries")`
   4. `servlet.setTransformWsdlLocations(true);`
      1. Sets whether relative address locations in the WSDL are to be transformed using 
         the request URI of the incoming `HttpServletRequest`. 
         Defaults to `false`.
      2. If you visit <http://localhost:8080/ws/countries.wsdl>, the `soap:address` will have the proper address. If you instead visit the WSDL from the public facing IP address assigned to your machine, you will see that address instead.

> [this project on github](https://github.com/peckwood/d171202-producting-web-service-spring-boot-soap-wsdl)
>
> [official Sping guide](https://spring.io/guides/gs/producing-web-service/)
>
> [official Sping guide project](https://github.com/spring-guides/gs-producing-web-service)
>
> [XML Schema Reference w3cschools](https://www.w3schools.com/xml/schema_elements_ref.asp)
>
> jaxb2
>
> - [stackoverflow: How to create the class java from WSDL in jaxb2-maven-plugin version 2?](https://stackoverflow.com/questions/44955247/how-to-create-the-class-java-from-wsdl-in-jaxb2-maven-plugin-version-2)
> - [stackoverflow: JAXB maven plugin not generating classes](https://stackoverflow.com/a/35243025/986966)
> - [memorynotfound: Generate Java Classes From XSD](https://memorynotfound.com/generate-java-classes-from-xsd/)
> - [official Basic XJC example](http://www.mojohaus.org/jaxb2-maven-plugin/Documentation/v2.2/example_xjc_basic.html)