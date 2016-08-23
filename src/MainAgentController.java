import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.util.ArrayList;
import java.util.Random;

public class MainAgentController {

    private static MainAgentController instance = null;
    private static jade.core.Runtime rt;
    private static jade.wrapper.AgentContainer mainContainer;
    private static AgentController controller, controller2;

    protected MainAgentController() throws ControllerException {


//        // Get a hold on JADE runtime
//        Runtime rt = Runtime.instance();
//// Create a default profile
//        Profile p = new ProfileImpl();
//// Create a new non-main container, connecting to the default
//// main container (i.e. on this host, port 1099)
//        ContainerController cc = rt.createAgentContainer(p);
//// Create a new agent, a DummyAgent
//// and pass it a reference to an Object
//        Object reference = new Object();
//        Object argums[] = new Object[1];
//        argums[0] = reference;
//        AgentController dummy = cc.createNewAgent("inProcess", "jade.tools.DummyAgent.DummyAgent", argums);
//// Fire up the agent
//        dummy.start();

        rt = Runtime.instance();

        Profile profile = new ProfileImpl();
//        profile.setParameter("gui", "true");

        mainContainer = rt.createMainContainer(profile);
        Object[] args = new String[2];
        args[0] = "Fender Stratocaster 1966";
        Random rand = new Random();
        args[1] = String.valueOf(rand.nextInt(90) + 10);

        controller2 = mainContainer.createNewAgent("Auctioneer", "AuctioneerAgent", args);
        ArrayList<AgentController> controllers = new ArrayList<>(2);
        for (int i = 0; i < 5; i++) {
            controllers.add(mainContainer.createNewAgent("bidder" + i, "BidderAgent", args));
        }

        try {
            for (AgentController agentController : controllers) {
                agentController.start();
            }

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        controller2.start();

    }

    public static MainAgentController getInstance() throws ControllerException {
        if (instance == null) {
            instance = new MainAgentController();
        }

        return instance;
    }

    public static void killInstance() throws StaleProxyException {
        controller.kill();
        controller2.kill();
        mainContainer.kill();
        rt.shutDown();
        instance = null;
    }
}

/**
 * >         // This is the important method. This launches the jade platform.
 * >         Runtime rt = Runtime.instance();
 * >
 * >         Profile profile = new ProfileImpl();
 * >
 * >         // With the Profile you can set some options for the container
 * >         profile.setParameter(Profile.PLATFORM_ID, "Platform Name");
 * >         profile.setParameter(Profile.CONTAINER_NAME, "Container Name");
 * >
 * >         // Create the Main Container
 * >         AgentContainer mainContainer = rt.createMainContainer(profile);
 * >
 * >         try {
 * >                 // Here I create an agent in the main container and start
 * > it.
 * >             AgentController ac = mainContainer.createNewAgent("manager",
 * >                     "ia.main.AgentManager", params);
 * >             ac.start();
 * >         } catch(StaleProxyException e) {
 * >             e.printStackTrace();
 * >         }
 * >     }
 */