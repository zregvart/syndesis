package io.syndesis.connector.sql;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;


public class CamelProcessor implements Processor {
    ProducerTemplate producer;
   
    public void setProducer(ProducerTemplate producer) {
      this.producer = producer;
    }
   
    public void process(Exchange inExchange) {
      // some loop for each message 
      String[] templates = {};
      for (String template : templates) {
         // lets send a new exchange to the producers default destination
         // being called back so we can customize the message
         producer.send(new Processor() {
            public void process(Exchange outExchange) {
                outExchange.getIn().setBody("This is the body"); 
                // set some headers too?
            }
         });
      }
  }
}
