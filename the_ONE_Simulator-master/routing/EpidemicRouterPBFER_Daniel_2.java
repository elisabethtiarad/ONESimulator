///* 
// * Copyright 2010 Aalto University, ComNet
// * Released under GPLv3. See LICENSE.txt for details. 
// */
//package routing;
//
//import core.Connection;
//import core.DTNHost;
//import core.Message;
//import core.Settings;
//import core.SimError;
//import core.Tuple;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//
///**
// * Epidemic message router with drop-oldest buffer and only single transferring
// * connections at a time.
// */
//public class EpidemicRouterPBFER_Daniel_2 extends ActiveRouter {
//
//    public static final String MESSAGE_PRIORITY = "priority";
//    public static final int Q_MODE_ASC = 1;
//
//    /**
//     * Constructor. Creates a new message router based on the settings in the
//     * given Settings object.
//     *
//     * @param s The settings object
//     */
//    public EpidemicRouterPBFER_Daniel_2(Settings s) {
//        super(s);
//    }
//
//    protected EpidemicRouterPBFER_Daniel_2(EpidemicRouterPBFER_Daniel_2 r) {
//        super(r);
//    }
//
//    @Override 
//	public boolean createNewMessage(Message m) {
//            return super.createNewMessage(m);	
//	}
//
//    //method ini masih aku pertanyakan dari mana? 
//    //soalnya di class EpidemicRouterPBFER dia sudah ada, nanti bisa dicari lagi
////    private void exchangeMessageProperty() {
////
////        Collection<Message> messCollection = getMessageCollection();
////
////        for (Connection conn : getConnections()) {
////            DTNHost other = conn.getOtherNode(getHost());
////            EpidemicRouterPBFER_Daniel_2 otherHost = (EpidemicRouterPBFER_Daniel_2) other.getRouter();
////
////            if (otherHost.isTransferring()) {
////                continue; //artinya di skip
////            }
////            //update replication number
////            for (Message m : messCollection) {
////                if (otherHost.hasMessage(m.getId())) {
////                    Message tmp = otherHost.getMessage(m.getId());
////                    Integer me = (Integer) m.getPriority();
////                    Integer peer = (Integer) tmp.getPriority();
////
////                    if (me < peer) {
////                        m.updateProperty(MESSAGE_PRIORITY, peer);
////                    }
////                }
////            }
////        }
////    }
//
//    protected List sortByQueueMode2(List list) {
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
//                        diff = (Integer) m1.getPriority() - (Integer) m2.getPriority();
//                        if (diff == 0) {
//                            return 0;
//                        }
//                        return (diff < 0 ? -1 : 1);
//                    }
//                });
//                break;
//            default:
//        }
//        return list;
//    }
//
//    protected Connection tryAllMessagesToAllConnections() {
//        List<Connection> connections = getConnections();
//
//        if (connections.size() == 0 || this.getNrofMessages() == 0) {
//            return null;
//        }
//
//        List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
//        List<Message> temp = new ArrayList<Message>();
////        exchangeMessageProperty();
//	sortByQueueMode2(messages); //kalau asli dari activeRouter memakai ini, tetapi punya Rosa tidak.
//
//        for (Connection c : connections) {
//            DTNHost otherhost = c.getOtherNode(getHost());
//            EpidemicRouterPBFER_Daniel_2 peer = (EpidemicRouterPBFER_Daniel_2) otherhost.getRouter();
//
//            if (getHost().getBufferOccupancy() < otherhost.getRouter().getFreeBufferSize()) {
//                temp.addAll(messages);
////                System.out.println(getHost().getBufferOccupancy()+"||"+otherhost.getRouter().getFreeBufferSize()); // untuk nge cek
//            } else if ((Integer) getHighestMessagePriority(true) > (Integer) peer.getLowestMessagePriority(true)) {
//                temp.addAll(messages);
//                System.out.println("High : "+getHighestMessagePriority(deleteDelivered)+" Low : "+getLowestMessagePriority(deleteDelivered));
//            } else if ((Integer) getLowestMessagePriority(true) < (Integer) peer.getHighestMessagePriority(true)) {
//                System.out.println("High : "+getLowestMessagePriority(deleteDelivered)+" Low : "+getHighestMessagePriority(deleteDelivered));
//                for (Message m : messages) {
//                    if (m.getSize() < peer.getFreeBufferSize()) {
//                        temp.add(m);
//                    }
//                }
//            } else {
//                List<Message> listMergedMessages = getMergedSortMessage(); //ini kalau masih eror karena methodnya belum dibuat
//                for (Message m : listMergedMessages) {
//                    if (!peer.hasMessage(m.getId()) && m.getSize() < peer.getFreeBufferSize()) {
//                        temp.add(m);
//                    }
//                }
//            }
//        }
//        this.sendQueueMode = Q_MODE_ASC;
//        List<Message> msgTemp = this.sortByQueueMode(temp);
//        return tryMessagesToConnections(msgTemp, connections);
//    }
//
//    @Override
//    public Message messageTransferred(String id, DTNHost from) {
//        Message m = super.messageTransferred(id, from);
//        return m;
//    }
//
//    //method ini untuk mencari nilai priority dari setiap pesan
//    private int getMessagePriority(Message m) {
//        return (Integer)m.getPriority();
//    }
//    
//    //method ini untuk mencari nilai rate dari setiap pesan
//    private Double getMessageRate(Message m) {
//        double rate;
//        double hopCount = m.getHopCount();
//        double ttlInit = this.msgTtl;
//        double ttlCurrent = m.getTtl();
//        rate = hopCount / (ttlInit - ttlCurrent);
//        return rate;
//    }
//
//    protected List<Message> getMergedSortMessage() {
//        List<Message> mergeMsg = new ArrayList<Message>();
//        for (Connection c : getConnections()) {
//            DTNHost thisHost = getHost();
//
//            DTNHost other = c.getOtherNode(thisHost);
//            EpidemicRouterPBFER_Daniel_2 peer = (EpidemicRouterPBFER_Daniel_2) other.getRouter();
//            List<Message> messageBufferA = new ArrayList<>(getMessageCollection());
//            List<Message> messageBufferB = new ArrayList<>(peer.getMessageCollection());
//
//            List<Message> m1 = this.sortByQueueMode(messageBufferA);
//            List<Message> m2 = peer.sortByQueueMode(messageBufferB);
//            mergeMsg.addAll(m1);
//            mergeMsg.addAll(m2);
//            Collections.sort(mergeMsg, new Comparator<Message>() {
//                @Override
//                public int compare(Message message1, Message message2) {
//                    int firstMessage = (Integer) message1.getPriority();
//                    int secondMessage = (Integer) message2.getPriority();
//
//                    if (firstMessage == secondMessage) {
//                        return 0;
//                    } else if (firstMessage > secondMessage) {
//                        return 1;
//                    } else {
//                        return -1;
//                    }
//                }
//            });
//
//        }
//        return mergeMsg;
//    }
//
//    protected Integer getHighestMessagePriority(boolean exludeMsgBeingSent) {
//        Collection<Message> messages = getMessageCollection();
//        Message biggestReplicationNumber = null;
//
//        // simpan copy
//        Integer highestCopies = 0;
//        for (Message m : messages) {
//            if (exludeMsgBeingSent && isSending(m.getId())) {
//                continue;
//            }
//            if (biggestReplicationNumber == null) {
//                biggestReplicationNumber = m;
//                highestCopies = (Integer) biggestReplicationNumber.getPriority();
////                System.out.println("IF - Nama Pesan Replication terbesar : "+biggestReplicationNumber);
////                System.out.println("IF - MESSAGE_PRIORITY : "+highestCopies);
//            } else if ((Integer) biggestReplicationNumber.getPriority() == (Integer) m.getPriority()) {
//                double firstMessage = getMessageRate(biggestReplicationNumber);
//                double secondMessage = getMessageRate(m);
//                if (firstMessage < secondMessage) {
//                    biggestReplicationNumber = m;
//                    highestCopies = (Integer) biggestReplicationNumber.getPriority();
////                    System.out.println("ELSE IF 1 - Nama Pesan Replication terbesar : "+biggestReplicationNumber);
////                    System.out.println("ELSE IF 1 - MESSAGE_PRIORITY : "+highestCopies);
//                }
//            } else if ((Integer) biggestReplicationNumber.getPriority() < (Integer) m.getPriority()) {
//                biggestReplicationNumber = m;
//                highestCopies = (Integer) biggestReplicationNumber.getPriority();
////                System.out.println("ELSE IF 2 - Nama Pesan Replication terbesar : "+biggestReplicationNumber);
////                System.out.println("ELSE IF 2 - MESSAGE_PRIORITY : "+highestCopies);
//            }
//
//        }
//
//        return highestCopies;
//    }
//
//    protected Integer getLowestMessagePriority(boolean exludeMsgBeingSent) {
//        Collection<Message> messages = getMessageCollection();
//        Message lowestReplicationNumber = null;
//
//        // simpan copy
//        Integer lowestCopies = 0;
//        for (Message m : messages) {
//            if (exludeMsgBeingSent && isSending(m.getId())) {
//                continue;
//            }
//            if (lowestReplicationNumber == null) {
//                lowestReplicationNumber = m;
//                lowestCopies = (Integer) lowestReplicationNumber.getPriority();
//            } else if ((Integer) lowestReplicationNumber.getPriority() == (Integer) m.getPriority()) {
//                double firstMessage = getMessageRate(lowestReplicationNumber);
//                double secondMessage = getMessageRate(m);
//                if (firstMessage > secondMessage) {
//                    lowestReplicationNumber = m;
//                    lowestCopies = (Integer) lowestReplicationNumber.getPriority();
//                }
//            } else if ((Integer) lowestReplicationNumber.getPriority() > (Integer) m.getPriority()) {
//                lowestReplicationNumber = m;
//                lowestCopies = (Integer) lowestReplicationNumber.getPriority();
//            }
//
//        }
//
//        return lowestCopies;
//
//    }
//
//    @Override
//    public void update() {
//        super.update();
//        if (isTransferring() || !canStartTransfer()) {
//            return; // transferring, don't try other connections yet
//        }
//
//        // Try first the messages that can be delivered to final recipient
//        if (exchangeDeliverableMessages() != null) {
//            return; // started a transfer, don't try others (yet)
//        }
//
//        // then try any/all message to any/all connection
//        this.tryAllMessagesToAllConnections();
//    }
//
//    @Override
//    public EpidemicRouterPBFER_Daniel_2 replicate() {
//        return new EpidemicRouterPBFER_Daniel_2(this);
//    }
//
//}
