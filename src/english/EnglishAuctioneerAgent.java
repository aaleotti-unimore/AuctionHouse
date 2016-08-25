package english;

import base.MainAgentController;
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

@SuppressWarnings({ "unchecked", "rawtypes", "unused"})
public class EnglishAuctioneerAgent extends Agent {
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
        printMessage(" is ready.");

        // Get the title of the Bidding item to Sell as a start-up argument and its initial price
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            itemName = (String) args[0];
            itemStartingPrice = Integer.valueOf((String) args[1]);
            printMessage("Target item is " + itemName + " and the starting price is " + itemStartingPrice);

            addBehaviour(new OneShotBehaviour(this) {
                @Override
                public void action() {
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
//                            doDelete();
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Perform the request
                    myAgent.addBehaviour(new AuctionPerformer());
                }
            });
        } else {
            // Make the agent terminate
            System.out.println("Bad Arguments");
//            doDelete();
        }

    }

    public boolean done() {
        return terminated;
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        printMessage(" terminating.");
    }

    private void printMessage(String msg) {
        System.out.println(getAID().getLocalName() + ": " + msg);
    }

    /**
     * Inner class. Performs the Auction Behaviour
     */
    private class AuctionPerformer extends Behaviour {
        private AID bestBidder = null; // The agent who provides the best offer
        private int step = 0;
        int repliesCnt = 0;
        private String conversationID = "Auction-for-" + itemName + System.currentTimeMillis();
        private ACLMessage cfp;
        private int currentItemPrice = itemStartingPrice;
        private int previousItemPrice = itemStartingPrice;
        private List<AID> proposingAgents = new ArrayList<>();

        public void action() {
            switch (step) {
                case 0:
                    // send Inform to all participants
                    ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                    participantAgents.forEach(inform::addReceiver);
                    inform.setContent(conversationID);
                    inform.setConversationId(conversationID);
                    myAgent.send(inform);
                    step = 1;
                    break;
                case 1:
                    repliesCnt = 0;

                    // Send cfp to all participants
                    printMessage(" call for " + itemName + " at price: " + currentItemPrice);
                    cfp = new ACLMessage(ACLMessage.CFP);
                    participantAgents.forEach(cfp::addReceiver);
                    cfp.setContent(String.valueOf(currentItemPrice));
                    cfp.setConversationId(conversationID);
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);
                    step = 2;
                    break;
                case 2:
                    // Receive all proposals/not-understood from the participants
                    MessageTemplate template = MessageTemplate.and(
                            MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), MessageTemplate.MatchPerformative(ACLMessage.NOT_UNDERSTOOD)),
                            MessageTemplate.and(MessageTemplate.MatchConversationId(cfp.getConversationId()), MessageTemplate.MatchInReplyTo(cfp.getReplyWith())));
                    ACLMessage reply = myAgent.receive(template);
                    if (reply != null) {
                        // Reply received fill array with current bidders
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            proposingAgents.add(reply.getSender());
                        }
                        repliesCnt++;
                    } else {
                        block();
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (repliesCnt >= participantAgents.size()) {
                        printMessage("received replies: " + repliesCnt);
                        // We received all replies
                        System.out.print("Proposing Agents: " + proposingAgents.size() + " List: ");
                        proposingAgents.forEach((a) -> System.out.print(a.getLocalName() + ", "));
                        System.out.println();
                        //new proposing agents
                        if (proposingAgents.size() > 0) {
                            bestBidder = proposingAgents.get(0);
                            step = 3;
                        }
                        //no new proposing agents and a previous best bidder has been chosen.
                        if (proposingAgents.size() <= 1 && bestBidder != null) step = 4;
                        if (proposingAgents.isEmpty() && bestBidder == null) {
                            printMessage("No bidders");
//                            doDelete();
                            MainAgentController.killInstance();
                        }
                    }
                    break;
                case 3:

                    // Send the accept-proposal to the first proposing agent
                    ACLMessage acceptBidderProposal = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    acceptBidderProposal.addReceiver(proposingAgents.get(0));
                    acceptBidderProposal.setConversationId(conversationID);
                    myAgent.send(acceptBidderProposal);
                    proposingAgents.remove(0);
                    ACLMessage rejectProposals = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                    rejectProposals.setConversationId(conversationID);
                    proposingAgents.forEach(rejectProposals::addReceiver);
                    myAgent.send(rejectProposals);

                    //clear the list, increase the price and restart from the cfp
                    proposingAgents = null;
                    proposingAgents = new ArrayList<>();
                    previousItemPrice = currentItemPrice;
                    currentItemPrice += 10;
                    step = 1;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case 4:
                    //inform everyone and request the winner
                    ACLMessage inform2 = new ACLMessage(ACLMessage.INFORM);
                    inform2.setConversationId("winner");
                    inform2.setContent(bestBidder.getLocalName());
                    participantAgents.remove(bestBidder);
                    participantAgents.forEach(inform2::addReceiver);
                    myAgent.send(inform2);

                    //request the winner to pay
                    ACLMessage requestWinner = new ACLMessage(ACLMessage.REQUEST);
                    requestWinner.setContent(Integer.toString(previousItemPrice));
                    requestWinner.setConversationId(conversationID);
                    requestWinner.addReceiver(bestBidder);
                    myAgent.send(requestWinner);

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    terminated = true;

//                    doDelete();
                    MainAgentController.killInstance();

                    break;
            }
        }

        public boolean done() {
            return terminated;
        }
    }  // End of inner class AuctionPerformer
}
