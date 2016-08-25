package base;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class MainAgentController {

    private static final int N_BIDDERS = 3;
    private static MainAgentController instance = null;
    private static jade.core.Runtime rt;
    private static jade.wrapper.AgentContainer mainContainer;
    private static AgentController snifferController;
    private static ArrayList<AgentController> biddersController;

    private MainAgentController() throws ControllerException, InterruptedException {
        rt = Runtime.instance();

        Profile profile = new ProfileImpl();
//        profile.setParameter("gui", "true");

        mainContainer = rt.createMainContainer(profile);
        Object[] snifferArgs = new Object[1];
        snifferArgs[0] = "Auctioneer;bidder0;bidder1;bidder2";
        Object[] biddersArgs = new Object[0];
        Object[] auctioneerArgs = new String[2];
        auctioneerArgs[0] = "1968 Fender Stratocaster";
        Random rand = new Random();
        auctioneerArgs[1] = String.valueOf(rand.nextInt(50) + 10);

        snifferController = mainContainer.createNewAgent("Sniffer", "jade.tools.sniffer.Sniffer", snifferArgs);


        System.out.println("Choose which Auction: English (E) / Dutch (D)");
        Scanner scanner = new Scanner(System.in);
        char c = scanner.next().charAt(0);
        AgentController auctioneerController = null;
        if ( c == 'E' || c == 'e') {
            auctioneerArgs[1] = String.valueOf(rand.nextInt(50) + 10);
            auctioneerController = mainContainer.createNewAgent("Auctioneer", "english.EnglishAuctioneerAgent", auctioneerArgs);
        } else if( c == 'D' || c == 'd')  {
            auctioneerArgs[1] = String.valueOf(100 - rand.nextInt(50));
            auctioneerController = mainContainer.createNewAgent("Auctioneer", "dutch.DutchAuctioneerAgent", auctioneerArgs);
        } else{
            System.out.println("Not valid option");
            System.exit(1);
        }

        snifferController.start();
        sleep(2000);
        biddersController = new ArrayList<>(3);
        for (int i = 0; i < N_BIDDERS; i++)
            biddersController.add(mainContainer.createNewAgent("bidder" + i, "base.BidderAgent", biddersArgs));

        for (AgentController agentController : biddersController) agentController.start();

        sleep(500);
        auctioneerController.start();

    }

    public static MainAgentController getInstance() throws ControllerException, InterruptedException {
        if (instance == null) {
            instance = new MainAgentController();
        }

        return instance;
    }

    public static void killInstance() {
        new Thread(() -> {
            try {
                System.out.println("Press enter to terminate");
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
                snifferController.kill();

                for (AgentController agentController : biddersController) agentController.kill();

                mainContainer.kill();
                rt.shutDown();
                instance = null;
                System.exit(0);
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }).start();

    }
}