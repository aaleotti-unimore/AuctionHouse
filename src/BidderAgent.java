/**
 * Created by Alessandro on 11/08/2016.
 */
public class BidderAgent {
}

/**
 * MessageTemplate template = MessageTemplate.and(
 * MessageTemplate.MatchProtocol("fipa-contract-net"),
 * MessageTemplate.MatchPerformative(ACLMessage.CFP) );
 *
 * addBehaviour(new CyclicBehaviour(this) {
 * public void action() {
 * ACLMessage cfp = myAgent.receive(template);
 * if (cfp != null) {
 * myAgent.addBehaviour(new SSContractNetResponder(myAgent, cfp)
 * {
 * // Redefine callback methods to implement domain-dependent
 * // logic
 * } );
 * }
 * else {
 * block();
 * }
 * }
 * } );
 */