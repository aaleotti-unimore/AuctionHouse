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

import java.util.ArrayList;
import java.util.List;

//@SuppressWarnings({ "unchecked", "rawtypes" })
public class AuctioneerAgent extends Agent {
    // The title and price of the item to sell
    private String itemName;
    private Integer itemStartingPrice;
    private Boolean terminated = false;

    // The list of known seller agents
    private List<AID> participantAgents;

    /**
     * agent initializations
     */
    protected void setup() {
        // Printout a welcome message
        System.out.println(" Auctioneer " + getAID().getName() + " is ready.");

        // Get the title of the Bidding item to Sell as a start-up argument and its initial price
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            itemName = (String) args[0];
            itemStartingPrice = Integer.valueOf((String) args[1]);
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
                        participantAgents = new ArrayList<>(result.length);
                        for (DFAgentDescription res : result) {
                            participantAgents.add(res.getName());
                            System.out.println("* " + res.getName().getLocalName());
                        }
                        if (result.length == 0) {
                            System.out.println("found no bidders. exiting");
                            terminated = true;
                            doDelete();
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

    public boolean done() {
        if (terminated) return true;
        else return false;
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Auctioneer-agent " + getAID().getName() + " terminating.");
    }

    /**
     * Inner class RequestPerformer.
     * This is the behaviour used by Book-buyer agents to request seller
     * agents the target item.
     */
    private class RequestPerformer extends Behaviour {
        private AID bestBidder; // The agent who provides the best offer
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private String conversationID = "Auction-for-" + itemName + System.currentTimeMillis();
        private ACLMessage cfp;
        private int currentItemPrice = itemStartingPrice;
        private List<AID> proposingAgents = new ArrayList<>();

        public void action() {
            switch (step) {
                case 0:
                    // send Inform to all participants

                    ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                    for (AID participant : participantAgents) {
                        inform.addReceiver(participant);
                    }
                    inform.setContent(conversationID);
                    inform.setConversationId(conversationID);
                    myAgent.send(inform);
                    proposingAgents = participantAgents;
                    step = 1;
                    break;
                case 1:
                    // Send cfp to all participants
                    System.out.println(myAgent.getName() + " call for " + itemName + "at price: " + currentItemPrice);
                    cfp = new ACLMessage(ACLMessage.CFP);
                    for (AID bidder : proposingAgents) {
                        cfp.addReceiver(bidder);
                    }
                    cfp.setContent(String.valueOf(currentItemPrice));
                    cfp.setConversationId(conversationID);
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value given he multiple CFP ongoing
//                    cfp.setReplyByDate(new Date(System.currentTimeMillis() + 2000));
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(conversationID),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
//                    step = 2;
//                    break;
//                case 2:
                    // Receive all proposals/not-understood from the participants

                    ACLMessage reply = myAgent.receive();
                    if (reply != null) {
                        // Reply received fill array with current bidders
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            proposingAgents.add(reply.getSender());
                            System.out.println(reply.getSender().getName() + " proposes");
                        }
                        if (reply.getPerformative() == ACLMessage.NOT_UNDERSTOOD) {
                            System.out.println(reply.getSender().getName() + " didn't understand");
                        }
                    } else {
                        block();
                    }
                    //wait until timeout
//                    if (new Date(System.currentTimeMillis()).after(cfp.getReplyByDate()))
                    if (proposingAgents.size() > 0) {
                        // We received at least one proposal
                        step = 3;
                    } else {
                        //no new proposals
                        step = 4;
                    }
                    break;
                case 3:
                    bestBidder = proposingAgents.get(0);

                    // Send the accept-proposal to the first proposing agent
                    ACLMessage acceptBidderProposal = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    acceptBidderProposal.addReceiver(proposingAgents.get(0));
                    acceptBidderProposal.setConversationId(conversationID);
                    myAgent.send(acceptBidderProposal);
                    System.out.println(proposingAgents.get(0).getName() + " is the current best bidder");

                    ACLMessage rejectProposals = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    for (AID agent : proposingAgents)
                        if (proposingAgents.indexOf(agent) > 0)
                            rejectProposals.addReceiver(agent);

                    rejectProposals.setConversationId(conversationID);
                    myAgent.send(rejectProposals);

                    //clear the list, increase the price and restart from the cfp
                    proposingAgents = null;
                    proposingAgents = new ArrayList<>();
                    currentItemPrice += 10;
                    step = 1;
                    break;
                case 4:
                    //inform everyone and request the winner
                    ACLMessage inform2 = new ACLMessage(ACLMessage.INFORM);
                    inform2.setContent("Auction won by " + bestBidder.getName());
                    System.out.println("Auction won by " + bestBidder.getName());
                    for (AID participant : participantAgents)
                        inform2.addReceiver(participant);
                    myAgent.send(inform2);

                    //request the winner to pay
                    ACLMessage requestWinner = new ACLMessage(ACLMessage.REQUEST);
                    requestWinner.setContent(Integer.toString(currentItemPrice));
                    requestWinner.addReceiver(bestBidder);
                    myAgent.send(requestWinner);
                    break;
            }
        }

        public boolean done() {
//            if (step == 3 && bestBidder == null) {
//                System.out.println("Attempt failed: " + itemName );
//            }
//            return ((step == 3 && bestBidder == null) || step == 4);
//        }
            return false;
        }
    }  // End of inner class RequestPerformer
}
