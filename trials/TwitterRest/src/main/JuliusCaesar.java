package main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;

import twitter.GetTrends;

public class JuliusCaesar {

	private static String propertiesMain = "properties/property.properties";
	public static BlockingQueue<OAuthConsumer> consumerPool;
	/*
	 * Adding a consumer Holder The reason being , usage of OAuth objects across
	 * all APPs spread equally
	 */
	public static int sizeOfConsumerPool;

	@SuppressWarnings("null")
	public static void main(String args[]) {

		Properties PropertyHandler = new Properties();
		Logger log = null;

		try {
			PropertyHandler.load(new FileInputStream(propertiesMain));

			consumerPool = ConsumerPool.buildConsumerPool();
			sizeOfConsumerPool = consumerPool.size();
			String logPath = PropertyHandler.getProperty("logPath");

			PropertyConfigurator.configure(logPath);
			log = Logger.getLogger(JuliusCaesar.class.getName());
			log.info("consumer pool created");

			/*
			 * Per trend how many calls per 15 mins and waiting time after each
			 * call
			 */

			int numberOfTrends = Integer.parseInt(PropertyHandler
					.getProperty("totalTrends"));
			int numberOfApps = Integer.parseInt(PropertyHandler
					.getProperty("numberOfAccounts"));
			int searchQueries = Integer.parseInt(PropertyHandler
					.getProperty("numberOfSearchQueries"));

			int totalSearchCalls = numberOfApps * searchQueries;
			int perTrend = (int) totalSearchCalls / numberOfTrends;

			double timeSpace = 15 / perTrend;
			double seconds = timeSpace * 60;
			int milliseconds = (int) seconds * 1000;

			// Adding executors

			int NumberOfThreads = Integer.parseInt(PropertyHandler
					.getProperty("numberOfThreads"));

			ExecutorService executor = Executors
					.newFixedThreadPool(NumberOfThreads);
			log.info("Executor class has been initialized");

			log.info("Started the timer to insert trends");
			/*
			 * This is the main logic here What happens is first it goes into
			 * the while loop Then creates the job stack Enters 2nd while If job
			 * stack is empty it waits then gets out of the loop else it takes a
			 * job assigns it to the executor this way i dont need to shutdown
			 * the executor at all
			 */
			while (true) {

				Stack<MrUrl> jobStack = MrMestri.buildJobs();
				int jobToken = 0;

				/*
				 * Removed all the complicate shit of timer
				 * and other conditions
				 * and finally figured out that all it needs is a basic if
				 * to get trends
				 * jobStack in this loop is empty if and only if there are no trends 
				 * for the day
				 * So when that happens
				 * Boom ! You go and get trends
				 * 
				 */
				if (jobStack.size() == 0){
					OAuthConsumer consumerObj = JuliusCaesar
							.getConsumerObject();
					GetTrends.retrieveTrends(consumerObj);
					log.info("Trends for the day has been added to the database");
					JuliusCaesar.putConsumerObject(consumerObj);
				}

				while (jobStack.size() > 0) {

					/*
					 * Now that the job stack is there we need to pick 80 jobs
					 * at a time and based on number of apps wait to run the
					 * process again
					 */

					MrRunnable worker = new MrRunnable(jobStack.pop());

					jobToken++;
					executor.execute(worker);

					if (jobToken
							% Integer.parseInt(PropertyHandler
									.getProperty("totalTrends")) == 0) {
						log.info("All jobs for the clock cycle complete , waiting for next clock cycle to start. Number of jobs completed "
								+ jobToken);
						log.info("Job stack size" + jobStack.size());
						Thread.sleep(milliseconds);

					}
				}

				if (jobStack.size() == 0) {
					log.info("Now waiting to re energize my apps ");
					Thread.sleep(120000);
				}

			}
		} catch (FileNotFoundException e) {
			log.error(e);
		} catch (InterruptedException e) {
			log.error(e);
		} catch (IOException e) {
			log.error(e);
		} catch (OAuthMessageSignerException e) {
			log.error(e);

		} catch (OAuthExpectationFailedException e) {
			log.error(e);

		} catch (OAuthCommunicationException e) {
			log.error(e);

		} catch (JSONException e) {
			log.error(e);

		} catch (SQLException e) {
			log.error(e);

		} catch (HttpException e) {
			log.error(e);

		} catch (ParseException e) {
			log.error(e);

		}
	}

	public static synchronized OAuthConsumer getConsumerObject()
			throws InterruptedException {
		OAuthConsumer consumerObj;
		consumerObj = consumerPool.take();
		return consumerObj;
	}

	public static synchronized void putConsumerObject(OAuthConsumer obj)
			throws InterruptedException {
		consumerPool.put(obj);
	}
}
