package org.mskcc.cmo.metadb;

import java.util.concurrent.CountDownLatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mskcc.cmo.messaging.Gateway;
import org.mskcc.cmo.metadb.service.MessageHandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.mskcc.cmo.messaging",
        "org.mskcc.cmo.common.*", "org.mskcc.cmo.metadb.*"})
public class MetadbRequestFilterApp implements CommandLineRunner {
    private static final Log LOG = LogFactory.getLog(MetadbRequestFilterApp.class);

    @Autowired
    private Gateway messagingGateway;

    @Autowired
    private MessageHandlingService messageHandlingService;

    private Thread shutdownHook;
    final CountDownLatch metadbRequestFilterAppClose = new CountDownLatch(1);

    @Override
    public void run(String... args) throws Exception {
        LOG.info("Starting up MetaDB Request Filter application...");
        try {
            installShutdownHook();
            messagingGateway.connect();
            messageHandlingService.initialize(messagingGateway);
            metadbRequestFilterAppClose.await();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }

    private void installShutdownHook() {
        shutdownHook =
            new Thread() {
                public void run() {
                    System.err.printf("\nCaught CTRL-C, shutting down gracefully...\n");
                    try {
                        messagingGateway.shutdown();
                        messageHandlingService.shutdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    metadbRequestFilterAppClose.countDown();
                }
            };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public static void main(String[] args) {
        SpringApplication.run(MetadbRequestFilterApp.class, args);
    }

}