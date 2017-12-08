package hello;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter{
	
	/*
	 * It’s important to notice that you need to specify bean names for  
	 * MessageDispatcherServlet and DefaultWsdl11Definition. Bean names 
	 * determine the URL under which web service and the generated WSDL 
	 * file is available. In this case, the WSDL will be available under
	 *  http://<host>:<port>/ws/countries.wsdl.
	 *  In this case: http://localhost:8080/ws/countries.wsdl
	 */
	
	@Bean
	public ServletRegistrationBean messageDispatcherServlet(ApplicationContext applicationContext){
		MessageDispatcherServlet servlet = new MessageDispatcherServlet();
		servlet.setApplicationContext(applicationContext);
		servlet.setTransformWsdlLocations(true);
		return new ServletRegistrationBean(servlet, "/ws/*");
	}
	
	@Bean
	public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema countriesSchema){
		DefaultWsdl11Definition wsdl11Definition= new DefaultWsdl11Definition();
		wsdl11Definition.setPortTypeName("CountriesPort");
		wsdl11Definition.setLocationUri("/ws");
		wsdl11Definition.setTargetNamespace("http://spring.io/guides/gs-producing-web-service");
		wsdl11Definition.setSchema(countriesSchema);
		return wsdl11Definition;
	}
	
	@Bean
	public XsdSchema countriesSchema(){
		return new SimpleXsdSchema(new ClassPathResource("countries.xsd"));
	}
}
