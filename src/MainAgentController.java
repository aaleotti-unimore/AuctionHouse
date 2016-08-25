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

class MainAgentController {

    private static final int N_BIDDERS = 3;
    private static MainAgentController instance = null;
    private static jade.core.Runtime rt;
    private static jade.wrapper.AgentContainer mainContainer;
    private static AgentController snifferController;
    private static AgentController auctioneerController;
    private static ArrayList<AgentController> biddersController;

    private MainAgentController() throws ControllerException, InterruptedException {
        rt = Runtime.instance();

        Profile profile = new ProfileImpl();
        profile.setParameter("gui", "true");

        mainContainer = rt.createMainContainer(profile);
        Object[] snifferArgs = new Object[1];
        snifferArgs[0] = "Auctioneer;bidder0;bidder1;bidder2";
        Object[] biddersArgs = new Object[0];
        Object[] auctioneerArgs = new String[2];
        auctioneerArgs[0] = "1968 Fender Stratocaster";
        Random rand = new Random();
        auctioneerArgs[1] = String.valueOf(rand.nextInt(50) + 10);

        snifferController = mainContainer.createNewAgent("Sniffer", "jade.tools.sniffer.Sniffer", snifferArgs);
        snifferController.start();
        sleep(2000);
        auctioneerController = mainContainer.createNewAgent("Auctioneer", "AuctioneerAgent", auctioneerArgs);
        biddersController = new ArrayList<>(3);
        for (int i = 0; i < N_BIDDERS ; i++)
            biddersController.add(mainContainer.createNewAgent("bidder" + i, "BidderAgent", biddersArgs));

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

    static void killInstance() {
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