/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimError;
import core.Tuple;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import net.sourceforge.jFuzzyLogic.rule.Variable;


/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class EpidemicRouterfu extends ActiveRouter {
    public static final String FCL_NAMES = "fcl";
    public static final String TIME_TO_LIVE = "ttl";
    public static final String FORWARD_TRANSMISSION_COUNT = "ftc";
    public static final String NILAI_PRIORITAS = "priority";
    public static final String MESSAGE_PRIORITY = "priority";
    public static final int Q_MODE_ASC = 1;

    
     private FIS fcl;
    /**
     * Constructor. Creates a new message router based on the settings in the
     * given Settings object.
     *
     * @param s The settings object
     */
     
    public EpidemicRouterfu(Settings s) {
        super(s);
        String fclString = s.getSetting(FCL_NAMES);
        fcl = FIS.load(fclString);
    }

    protected EpidemicRouterfu(EpidemicRouterfu r) {
        super(r);
        this.fcl = r.fcl;
    }

    @Override
    public boolean createNewMessage(Message m) { 
        m.setFtc(m.getHopCount());
        m.setTtl(this.msgTtl);
        m.addProperty(MESSAGE_PRIORITY, 1);
        return super.createNewMessage(m);
    }
    
       
    public  double callPriority(Message m) {       
            m.coaValue = fuzzy(m);
            m.priority = 1 - m.coaValue;       
    return m.priority;
         
    }
    
    
    

    //jika pesan sama, nilai replikasi akan di set ke replikasi tertinggi
    private void exchangeMessageProperty() {

        Collection<Message> messCollection = getMessageCollection();

        for (Connection conn : getConnections()) {
            DTNHost other = conn.getOtherNode(getHost());
            EpidemicRouterfu otherHost = (EpidemicRouterfu) other.getRouter();

            if (otherHost.isTransferring()) {
                continue; //artinya di skip
            }
            //update jumlah replikasi
            for (Message m : messCollection) {
                if (otherHost.hasMessage(m.getId())) {
                    Message tmp = otherHost.getMessage(m.getId());
                    Integer me = (Integer) m.getProperty(MESSAGE_PRIORITY);                  
                    Integer peer = (Integer) tmp.getProperty(MESSAGE_PRIORITY);

                    if (me < peer) {
                        m.updateProperty(MESSAGE_PRIORITY, peer);
                    }
                }
            }
        }
    }

    @Override
//    protected List sortByQueueMode(List list) {
//        switch (sendQueueMode) {
//            case Q_MODE_ASC:
//                Collections.sort(list, new Comparator() {
//
//                    @Override
//                    public int compare(Object o1, Object o2) {
//                        double diff;
//                        Message m1, m2;
//
//                        if (o1 instanceof Tuple) {
//                            m1 = ((Tuple<Message, Connection>) o1).getKey();
//                            m2 = ((Tuple<Message, Connection>) o2).getKey();
//                        } else if (o1 instanceof Message) {
//                            m1 = (Message) o1;
//                            m2 = (Message) o2;
//                        } else {
//                            throw new SimError("Invalid type of objects in "
//                                    + "the list");
//                        }
//
//                        diff = (Integer) m1.getProperty(MESSAGE_PRIORITY) - (Integer) m2.getProperty(MESSAGE_PRIORITY);
//                        if (diff == 0) {
//                            return 0;
//                        }
//                        return (diff < 0 ? -1 : 1);
//                    }
//                });
//                break;
//            default:
//
//        }
//
//        return list;
//    }

   // @Override
    protected Connection tryAllMessagesToAllConnections() {
        List<Connection> connections = getConnections();

        if (connections.size() == 0 || this.getNrofMessages() == 0) {
            return null;
        }

        List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
        List<Message> temp = new ArrayList<Message>();
        exchangeMessageProperty();
		this.sortByQueueMode(messages); //kalau asli dari activeRouter memakai ini.

        for (Connection c : connections) {
            DTNHost otherhost = c.getOtherNode(getHost());
            EpidemicRouterfu peer = (EpidemicRouterfu) otherhost.getRouter();

            if (getHost().getBufferOccupancy() < otherhost.getRouter().getFreeBufferSize()) {
                temp.addAll(messages);
//                System.out.println(getHost().getBufferOccupancy()+"||"+otherhost.getRouter().getFreeBufferSize()); // untuk nge cek
            } else if ((Integer) getHighestMessageReplication(true) > (Integer) peer.getLowestMessageReplication(true)) {
                temp.addAll(messages);
             //   System.out.println("High : " + getHighestMessageReplication(deleteDelivered) + " Low : " + getLowestMessageReplication(deleteDelivered));
            } else if ((Integer) getLowestMessageReplication(true) < (Integer) peer.getHighestMessageReplication(true)) {
                for (Message m : messages) {
                    if (m.getSize() < peer.getFreeBufferSize()) {
                        temp.add(m);  
                    }
                }
            } else {
                List<Message> listMergedMessages = getMergedSortMessage(); 
                for (Message m : listMergedMessages) {
                    if (!peer.hasMessage(m.getId()) && m.getSize() < peer.getFreeBufferSize()) {
                        temp.add(m);
                    }
                }
            }
        }
        this.sendQueueMode = Q_MODE_ASC;
        List<Message> msgTemp = this.sortByQueueMode(temp);
        return tryMessagesToConnections(msgTemp, connections);
    }

    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);
        Integer count = (Integer) m.getProperty(MESSAGE_PRIORITY) + 1;
        m.updateProperty(MESSAGE_PRIORITY, count);
        return m;
    }

    //jika nilai replikasi sama, maka menggunakan ini
    private int getSecurityUrgentMessage(Message m) {
        int priority;
        int security = m.getSecurity();
        int urgency = m.getUrgency();
        priority = (security + urgency) / 2;     
     //  System.out.println("Security : " + security + " | Urgency : " + urgency + " | Priority : " + priority);
       // System.out.println("FTC : " + FTC +" dan" + " TTL : " + TTL);
       // fuzzy(m);
      //  System.out.println("coba eli : " + m.fuzzy(m));
      callPriority(m);
     // System.out.println("FTC : " + m.getFtc()+" dan" + " TTL : " + m.getTtl() + " memiliki priority : " +  callPriority(m));
        
        return priority;
    }
    
    protected double fuzzy (Message m){
        double ftcValue = m.getFtc();
        double ttlValue = m.getTtl();
        FunctionBlock functionBlock = fcl.getFunctionBlock(null);
        

        functionBlock.setVariable(FORWARD_TRANSMISSION_COUNT, ftcValue);
        functionBlock.setVariable(TIME_TO_LIVE, ttlValue);
        functionBlock.evaluate();

        Variable coa = functionBlock.getVariable(NILAI_PRIORITAS);

       //double priority = 1 - coa.getValue();
       // System.out.println(functionBlock);
      //  System.out.println("coa : " + coa.getValue());
      //  JFuzzyChart.get().chart(functionBlock);
        return coa.getValue();
    }



    protected List<Message> getMergedSortMessage() {
        List<Message> mergeMsg = new ArrayList<Message>();
        for (Connection c : getConnections()) {
            DTNHost thisHost = getHost();

            DTNHost other = c.getOtherNode(thisHost);
            EpidemicRouterfu peer = (EpidemicRouterfu) other.getRouter();
            List<Message> messageBufferA = new ArrayList<>(getMessageCollection());
            List<Message> messageBufferB = new ArrayList<>(peer.getMessageCollection());
            
               

            List<Message> m1 = this.sortByQueueMode(messageBufferA);
            List<Message> m2 = peer.sortByQueueMode(messageBufferB);
            mergeMsg.addAll(m1);           
            mergeMsg.addAll(m2);
            Collections.sort(mergeMsg, new Comparator<Message>() {
                @Override
                public int compare(Message message1, Message message2) {
                    int firstMessage = (Integer) message1.getProperty(MESSAGE_PRIORITY);
                    int secondMessage = (Integer) message2.getProperty(MESSAGE_PRIORITY);
                    if (firstMessage == secondMessage) {
                        return 0;
                    } else if (firstMessage > secondMessage) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });

        }
        
        return mergeMsg;
    }
   
    protected Integer getHighestMessageReplication(boolean exludeMsgBeingSent) {
        Collection<Message> messages = getMessageCollection();
        Message biggestReplicationNumber = null;

        // simpan copy
        Integer highestCopies = 0;
        for (Message m : messages) {
            if (exludeMsgBeingSent && isSending(m.getId())) {
                continue;
            }
            if (biggestReplicationNumber == null) {
                biggestReplicationNumber = m;
                highestCopies = (Integer) biggestReplicationNumber.getProperty(MESSAGE_PRIORITY);
//                System.out.println("IF - Nama Pesan Replication terbesar : " + biggestReplicationNumber);
//                System.out.println("IF - jumlah copy terbanyak : " + highestCopies);
                
            } else if ((Integer) biggestReplicationNumber.getProperty(MESSAGE_PRIORITY) == (Integer) m.getProperty(MESSAGE_PRIORITY)) {
                int firstMessage = getSecurityUrgentMessage(biggestReplicationNumber);
              //  fuzzy(biggestReplicationNumber);
                int secondMessage = getSecurityUrgentMessage(m);
                if (firstMessage < secondMessage) {
                    biggestReplicationNumber = m;
                    highestCopies = (Integer) biggestReplicationNumber.getProperty(MESSAGE_PRIORITY);
//                    System.out.println("ELSE IF 1 - Nama Pesan Replication terbesar : " + biggestReplicationNumber);
//                    System.out.println("ELSE IF 1 - jumlah copy terbanyak: " + highestCopies);
                }
            } else if ((Integer) biggestReplicationNumber.getProperty(MESSAGE_PRIORITY) < (Integer) m.getProperty(MESSAGE_PRIORITY)) {
                biggestReplicationNumber = m;
                highestCopies = (Integer) biggestReplicationNumber.getProperty(MESSAGE_PRIORITY);
//                System.out.println("ELSE IF 2 - Nama Pesan Replication terbesar : " + biggestReplicationNumber);
//                System.out.println("ELSE IF 2 - jumlah copy terbanyak : " + highestCopies);
            }

        }

        return highestCopies;
    }

    protected Integer getLowestMessageReplication(boolean exludeMsgBeingSent) {
        Collection<Message> messages = getMessageCollection();
        Message lowestReplicationNumber = null;

        // simpan copy
        Integer lowestCopies = 0;
        for (Message m : messages) {
            if (exludeMsgBeingSent && isSending(m.getId())) {
                continue;
            }
            if (lowestReplicationNumber == null) {
                lowestReplicationNumber = m;
                lowestCopies = (Integer) lowestReplicationNumber.getProperty(MESSAGE_PRIORITY);
            } else if ((Integer) lowestReplicationNumber.getProperty(MESSAGE_PRIORITY) == (Integer) m.getProperty(MESSAGE_PRIORITY)) {
                double firstMessage = getSecurityUrgentMessage(lowestReplicationNumber);
                double secondMessage = getSecurityUrgentMessage(m);
                if (firstMessage > secondMessage) {
                    lowestReplicationNumber = m;
                    lowestCopies = (Integer) lowestReplicationNumber.getProperty(MESSAGE_PRIORITY);
                }
            } else if ((Integer) lowestReplicationNumber.getProperty(MESSAGE_PRIORITY) > (Integer) m.getProperty(MESSAGE_PRIORITY)) {
                lowestReplicationNumber = m;
                lowestCopies = (Integer) lowestReplicationNumber.getProperty(MESSAGE_PRIORITY);
            }

        }

        return lowestCopies;

    }

    @Override
    public void update() {
        super.update();
        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }

        // Try first the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }

        // then try any/all message to any/all connection
        this.tryAllMessagesToAllConnections();

    
    }
    
   @Override
    public EpidemicRouterfu replicate() {
        return new EpidemicRouterfu(this);
    }  
}



