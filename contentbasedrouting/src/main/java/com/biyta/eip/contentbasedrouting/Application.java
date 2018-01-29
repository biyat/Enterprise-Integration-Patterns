package com.biyta.eip.contentbasedrouting;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@EnableAutoConfiguration
public class Application {

	@RequestMapping("/")
	String home() {
		return "Hello World!";
	}

	/**
	 * No need to define Camel Context, since this is auto configured based on included library. 
	 * If we want to use it, we just need to autowire.
	 */
	@Autowired
	CamelContext camelContext;

	/**
	 * Spring boot main code execution.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * When configuration are initialized we want to let Camel know what do i.e all rules 
	 * must be configured so that Camel can use them to act on the input events.
	 * @return
	 */
	@Bean
	RoutesBuilder camelRouter() {
		return new RouteBuilder() {
			/**
			 * Configure routing Rules. Use Camel route to get data from one Rabbit MQ Queue to another i.e. create a
			 * bridge.
			 * @throws Exception
			 */
			@Override
			public void configure() throws Exception {

				from("rabbitmq://localhost/process-exchange?username=guest&password=guest"
						+ "&queue=order-queue"
						+ "&routingKey=medical-supplies"
						+ "&autoDelete=false&autoAck=true")
						.removeHeaders("rabbitmq.*").throttle(100).timePeriodMillis(10000).process(new Processor() {
							public void process(Exchange exchange) throws Exception {
								exchange.getOut().getHeaders().put("rabbitmq.EXCHANGE_NAME", "emergency-exchange");
								exchange.getOut().getHeaders().put("rabbitmq.ROUTING_KEY", "prime-orders");
								exchange.getOut().setBody(exchange.getIn().getBody());

							}
						})
						.to("rabbitmq://localhost/emergency-exchange?username=guest&password=guest"
								+ "&routingKey=prime-orders&autoDelete=false");
								
			}
		};
	}	
}
