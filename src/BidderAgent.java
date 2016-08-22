import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;

/**
 * Created by Alessandro on 11/08/2016.
 */
public class BidderAgent extends Agent {
    int myCash;
    Boolean terminated = false;

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
        if (terminated) return true;
        else return false;
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

//    private class OfferRequestsServer extends CyclicBehaviour {
//        public void action() {
//            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
//            ACLMessage msg = myAgent.receive(mt);
//
//            if (msg != null) {
//                // CFP Message received. Process it
//                String title = msg.getContent();
//                ACLMessage reply = msg.createReply();
//
//                Integer price = (Integer) catalogue.get(title);
//                if (price != null) {
//                    // The requested book is available for sale. Reply with the price
//                    reply.setPerformative(ACLMessage.PROPOSE);
//                    reply.setContent(String.valueOf(price.intValue()));
//                } else {
//                    // The requested book is NOT available for sale.
//                    reply.setPerformative(ACLMessage.REFUSE);
//                    reply.setContent("not-available");
//                }
//                myAgent.send(reply);
//            } else {
//                block();
//            }
//        }
//    }  // End of inner class OfferRequestsServer

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
                    ACLMessage reply = msg.createReply();
                    reply.setConversationId(conversationID);

                    //TODO GUARDACI DENTRO
                    reply.setInReplyTo(msg.getReplyWith());

                    if (msg.getPerformative() == ACLMessage.CFP) {
                        currentItemValue = Integer.valueOf(msg.getContent());
                        if (currentItemValue < myCash) {
                            reply.setPerformative(ACLMessage.PROPOSE);
                            printMessage("I Propose");
                        } else {
                            reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                            doDelete();
                        }
                    }

                    if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        printMessage("I'm the best bidder!");
                    }

                    if (msg.getPerformative() == ACLMessage.REQUEST) {
                        currentItemValue = Integer.valueOf(msg.getContent());
                        myCash = -currentItemValue;
                        printMessage("Payed the item " + currentItemValue + " current wallet is: " + myCash);
                        doDelete();
                    }
                } catch (NumberFormatException e) {
                    System.out.println("This is not a number");
                    System.out.println(e.getMessage());
                }
            }

        }
    }
}