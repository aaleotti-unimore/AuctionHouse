import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;

public class BidderAgent extends Agent {
    private int myCash;
    private Boolean terminated = false;

    @Override
    protected void setup() {
        Random rand = new Random();
        myCash = rand.nextInt(90) + 10;
        printMessage("my wallet is " + myCash);
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bidder");
        sd.setName("bidder#" + System.currentTimeMillis());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Add the behaviour serving queries from buyer agents
        addBehaviour(new BidPerformer());
    }

    public boolean done() {
        return terminated;
    }


    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        printMessage("terminating");
    }

    private void printMessage(String msg) {
        System.out.println(getAID().getLocalName() + ": " + msg);
    }

    private class BidPerformer extends CyclicBehaviour {
        private String conversationID;
        int currentItemValue;

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                try {
                    conversationID = msg.getConversationId();


                    if (msg.getPerformative() == ACLMessage.CFP) {
                        ACLMessage reply = msg.createReply();
                        reply.setConversationId(conversationID);
                        currentItemValue = Integer.valueOf(msg.getContent());
                        if (currentItemValue <= myCash) {
                            printMessage("Proposing. Offer is " + currentItemValue + " wallet is " + myCash + " Remaining cash: " + (myCash - currentItemValue));
                            reply.setPerformative(ACLMessage.PROPOSE);
                        } else {
                            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                        }
                        myAgent.send(reply);
                    }

                    if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
                        printMessage("I'm the best bidder!");
                    }

                    if (msg.getPerformative() == ACLMessage.INFORM && conversationID.equals("winner")) {
                        printMessage("Received winner's name: " + msg.getContent());
//                        doDelete();
                    }


                    if (msg.getPerformative() == ACLMessage.REQUEST) {
                        currentItemValue = Integer.valueOf(msg.getContent());
                        myCash -= currentItemValue;
                        printMessage("Payed the item " + currentItemValue + " current wallet is: " + myCash);
                        terminated = true;
//                        doDelete();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("This is not a number");
                    System.out.println(e.getMessage());
                }
            }

        }
    }
}