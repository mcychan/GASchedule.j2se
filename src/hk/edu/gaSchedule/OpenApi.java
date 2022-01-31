package hk.edu.gaSchedule;

import hk.edu.gaSchedule.algorithm.Amga2;
import hk.edu.gaSchedule.algorithm.Configuration;
import hk.edu.gaSchedule.algorithm.Schedule;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

public class OpenApi {
	public static void main(String[] args) {
		Undertow server = Undertow.builder()
			.addHttpListener(1953, "127.0.0.1")
			.setHandler(new BlockingHandler(new HttpHandler() {
				public void handleRequest(HttpServerExchange exchange) throws Exception {					
					exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
					exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Origin"), "*");					
					
					exchange.getRequestReceiver().receiveFullBytes((e, m) -> {
						try {
							Configuration configuration = new Configuration();
							configuration.parse(new String(m));
							// GeneticAlgorithm<Schedule> alg = new GeneticAlgorithm<>(new Schedule(configuration), 2, 2, 80, 3);
							Amga2<Schedule> alg = new Amga2<>(new Schedule(configuration), 0.35f, 2, 80, 3);
							System.out.println("\n");
							System.out.println(String.format("GaSchedule Version %s . Making a Class Schedule Using %s.", "1.2.0", alg.toString()));
							System.out.println("Copyright (C) 2022 Miller Cy Chan.");
							alg.run(9999, 0.999);

							String result = null;
							if(exchange.getRequestPath().endsWith("html"))
								result = HtmlOutput.getResult(alg.getResult());
							else if(exchange.getRequestPath().endsWith("json"))
								result = JsonOutput.getResult(alg.getResult());
							exchange.setStatusCode(StatusCodes.OK);

							if(result != null)
								exchange.getResponseSender().send(result);

						} catch (Exception ex) {
							ex.printStackTrace();
						}
				    });					
				}
			})).build();
		server.start();
	}

}
