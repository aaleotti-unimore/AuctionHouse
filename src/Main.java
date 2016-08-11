import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

public class Main {

    public static void main(String[] args) throws ControllerException, InterruptedException {

        MainAgentController ac = MainAgentController.getInstance();
        Thread.sleep(5000);
        ac.killInstance();
        System.exit(0);
    }
}


