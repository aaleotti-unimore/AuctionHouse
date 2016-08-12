import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class AuctioneerAgent extends Agent {
    // The title and price of the item to sell
    private String itemName;
    private Integer itemStartingPrice;

    // The list of known seller agents
    private AID[] bidderAgents;

    // Put agent initializations here
    protected void setup() {
        // Printout a welcome message
        System.out.println(" Auctioneer " + getAID().getName() + " is ready.");

        // Get the title of the Bidding item to Sell as a start-up argument and its initial price
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            itemName = (String) args[0];
            itemStartingPrice = (Integer) args[1];
            System.out.println("Target item is " + itemName + " and the starting price is " + itemStartingPrice);

            addBehaviour(new OneShotBehaviour(this) {
                @Override
                public void action() {
                    System.out.println("Trying to sell " + itemName);
                    // Update the list of bidding agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("bidder");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found the following bidders:");
                        bidderAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            bidderAgents[i] = result[i].getName();
                            System.out.println(bidderAgents[i].getName());
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Perform the request
                    myAgent.addBehaviour(new RequestPerformer());
                }
            });
        } else {
            // Make the agent terminate
            System.out.println("Bad Arguments");
            doDelete();
        }
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Auctioneer-agent " + getAID().getName() + " terminating.");
    }

    /**
     * Inner class RequestPerformer.
     * This is the behaviour used by Book-buyer agents to request seller
     * agents the target book.
     */
    private class RequestPerformer extends Behaviour {
        private AID bestSeller; // The agent who provides the best offer
        private int bestPrice;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private String ConversationID = "Auction-for-" + itemName;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all bidders
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < bidderAgents.length; ++i) {
                        cfp.addReceiver(bidderAgents[i]);
                    }
                    cfp.setContent(String.valueOf(itemStartingPrice));
                    cfp.setConversationId("auction");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(ConversationID),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer
                            int price = Integer.parseInt(reply.getContent());
                            if (bestSeller == null || price < bestPrice) {
                                // This is the best offer at present
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= bidderAgents.length) {
                            // We received all replies
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(itemName);
                    order.setConversationId(ConversationID);
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(ConversationID),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(itemName + " successfully purchased from agent " + reply.getSender().getName());
                            System.out.println("Price = " + bestPrice);
                            myAgent.doDelete();
                        } else {
                            System.out.println("Attempt failed: requested book already sold.");
                        }

                        step = 4;
                    } else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println("Attempt failed: " + itemName + " not available for sale");
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }  // End of inner class RequestPerformer
}
