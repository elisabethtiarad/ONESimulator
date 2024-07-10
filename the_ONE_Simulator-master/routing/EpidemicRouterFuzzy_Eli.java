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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
 * Epidemic message router with drop-oldest buffer and only single transferring
 * connections at a time.
 */
public class EpidemicRouterFuzzy_Eli extends ActiveRouter {

    public static final String FCL_NAMES = "fcl";
    public static final String TIME_TO_LIVE = "ttl";
    public static final String FORWARD_TRANSMISSION_COUNT = "ftc";
    public static final String MESSAGE_PRIORITY = "priority";
    public static final int Q_MODE_DESC = 1;
    private List<Message> ackList;
    private Set<String> allAckedMessages;

    private FIS fcl;

    /**
     * Constructor. Creates a new message router based on the settings in the
     * given Settings object.
     *
     * @param s The settings object
     */
    public EpidemicRouterFuzzy_Eli(Settings s) {
        super(s);
        String fclString = s.getSetting(FCL_NAMES);
        fcl = FIS.load(fclString);
        this.ackList = new ArrayList<Message>();
        this.allAckedMessages = new HashSet<>();
        //TODO: read&use epidemic router specific settings (if any)
    }

    /**
     * Copy constructor.
     *
     * @param r The router prototype where setting values are copied from
     */
    protected EpidemicRouterFuzzy_Eli(EpidemicRouterFuzzy_Eli r) {
        super(r);
        this.fcl = r.fcl;
        this.ackList = new ArrayList<>(r.ackList);
        this.allAckedMessages = new HashSet<>(r.allAckedMessages);
        //TODO: copy epidemic settings here (if any)
    }

    @Override
    public boolean createNewMessage(Message m) {
        m.setFtc(m.getHopCount());
        m.setTtl(this.msgTtl);
        return super.createNewMessage(m);
    }

    // method ini berisi daftar pesan yang sudah sampai ke tujuan akhir yang diketahui oleh setiap node
    protected Message createAckedList(Message m, DTNHost node) {
        if (isDeliveredMessage(m)) { //benar jika pesan dengan ID yang sama telah diterima oleh host ini sebagai penerima akhir.
            ackList.add(m); // tambahkan m ke ackList
            allAckedMessages.add(m.getId());// id dari pesan m ditambahkan
        }
        return m;
    }

    // Pertukaran daftar pesan yang diakui antar node
    public void exchangeAckedList() {
        List<Connection> connections = getConnections();//mendapatkan daftar koneksi yang tersedia
        List<String> myAckedMessages = new LinkedList<>(this.deliveredMessages.keySet());
        // daftar id pesan yng sudah sampai ditujuan yang dketahui node ini

        for (Connection con : connections) {
            MessageRouter otherRouter = con.getOtherNode(getHost()).getRouter(); // other node
            List<String> otherAckedMessages = new LinkedList<>(otherRouter.deliveredMessages.keySet());
            // daftar id pesan yang sudah sampai ketujuan yang diketahui other node
            for (String ackedMessage : otherAckedMessages) {// untuk setiap ID pesan yang sudah sampai ketujuan yang diketahui oleh otherRouter.
                if (!this.deliveredMessages.containsKey(ackedMessage)) { // jika pesan tidak ada di this node
                    this.deliveredMessages.put(ackedMessage, null); // tambahkan ke deliveredMessage
                }
            }
            for (String ackedMessage : myAckedMessages) { // untuk setiap ID pesan yang sudah sampai ketujuan yang diketahui oleh this node
                if (!otherRouter.deliveredMessages.containsKey(ackedMessage)) {// jika pesan tidak ada di other router
                    otherRouter.deliveredMessages.put(ackedMessage, null); // tambahkan ke deliveredMessage
                }
            }
        }
    }

    // method ini untuk menghapus ackedList atau pesan yang sudah sampai ke tujuan
    public void deleteAckedList() {

        Collection<Message> messCollection = getMessageCollection();// mendapatkan semua pesan
        List<String> messagesToDelete = new LinkedList<>();// daftar pesan untuk dihapus

        for (Message message : messCollection) {// untuk setiap pesan yang ada di messagecollestion
            if (isDeliveredMessage(message) || allAckedMessages.contains(message.getId())) {
                // jika pesan tersebut sudah sampai ke tujuan atau apakah id pesan tersebut berada di allackedMessage
                messagesToDelete.add(message.getId());// maka tambahkan ke daftar pesan yang akan dihapus
            }
        }
        for (String messageId : messagesToDelete) {
            deleteMessage(messageId, true); // ini untuk menghapus pesan
        }
    }

    // deliverable message adalah pesan yang tujuannya node yang terhubung
    @Override
    protected Connection exchangeDeliverableMessages() {
        List<Connection> connections = getConnections();// mendapatkan daftar koneksi
        List<Message> deliverableMessages = new ArrayList<>();// daftar deliverable message untuk menyimpan pesan yang dpt dikirim

        if (connections.size() == 0) {// jika tidak ada koneksi maka return null
            return null;
        }
        // mengambil pesan yang siap untuk dikirim
        //Metode getMessagesForConnected untuk mengumpulkan pasangan pesan dan koneksi dimana tujuan pesan cocok dng tujuan koneksi
        List<Tuple<Message, Connection>> sortedMessagesForConnected = sortByQueueMode(getMessagesForConnected());
        //Mencoba mengirim pesan yang telah diurutkan ke koneksi dengan memanggil tryMessagesForConnected(sortedMessagesForConnected).
        Tuple<Message, Connection> t = tryMessagesForConnected(sortedMessagesForConnected);

        if (t != null) {// jika t tidak null
            Message m = t.getKey(); // maka tambahkan pesan ke deliverable
            deliverableMessages.add(m);

            if (deliverableMessages.size() > 1) { // jika lebih dari satu hitung priority
                callPriority(deliverableMessages);
                List<Message> sortedDeliverableMessages = this.sortByQueueMode(deliverableMessages);
                return tryMessagesToConnections(sortedDeliverableMessages, connections);
            } else {// jika suma satu return t.getValue
                return t.getValue();
            }
        }

        for (Connection con : connections) {
            if (con.getOtherNode(getHost()).requestDeliverableMessages(con)) {
                return con;
            }
        }

        return null;
    }

    protected double fuzzy(Message m) {
        double ftcValue = m.getFtc();
        double ttlValue = m.getTtl();
        FunctionBlock functionBlock = fcl.getFunctionBlock(null);
        functionBlock.setVariable(FORWARD_TRANSMISSION_COUNT, ftcValue);
        functionBlock.setVariable(TIME_TO_LIVE, ttlValue);
        functionBlock.evaluate();
        Variable coa = functionBlock.getVariable(MESSAGE_PRIORITY);
        return coa.getValue();
    }

    public List<Message> callPriority(List<Message> msgs) {
        for (Message msg : msgs) {
            msg.coaValue = fuzzy(msg);
            double priority = 1 - msg.coaValue;
            msg.setPriority(priority);
        }
        return msgs;
    }

    @Override
    protected List sortByQueueMode(List list) {
        switch (sendQueueMode) {
            case Q_MODE_DESC:
                Collections.sort(list, new Comparator() {

                    @Override
                    public int compare(Object o1, Object o2) {
                        double diff;
                        Message m1, m2;

                        if (o1 instanceof Tuple) {
                            m1 = ((Tuple<Message, Connection>) o1).getKey();
                            m2 = ((Tuple<Message, Connection>) o2).getKey();
                        } else if (o1 instanceof Message) {
                            m1 = (Message) o1;
                            m2 = (Message) o2;
                        } else {
                            throw new SimError("Invalid type of objects in "
                                    + "the list");
                        }

                        diff = m2.getPriority() - m1.getPriority();
                        if (diff == 0) { // jika prioritas sama return 0
                            return 0;
                        }
                        return (diff < 0 ? -1 : 1); // jika m2 lebih tinggi return -1, jika m1 lebih tinggi return 1
                    }
                });
                break;
            default:
                // Default case can be handled here if needed
                break;
        }

        return list;
    }

    @Override
    protected Connection tryAllMessagesToAllConnections() {
        List<Connection> connections = getConnections();

        if (connections.size() == 0 || this.getNrofMessages() == 0) {
            return null;
        }      
        List<Message> mergeMsg = getMergedSortMessage();
        List<Message> temp = new ArrayList<Message>();       

        for (Connection c : connections) {
            DTNHost otherHost = c.getOtherNode(getHost());
            EpidemicRouterFuzzy_Eli peer = (EpidemicRouterFuzzy_Eli) otherHost.getRouter();
            for (Message m : mergeMsg) {
                if (!peer.hasMessage(m.getId()) && m.getSize() < peer.getFreeBufferSize()) {
                    temp.add(m);
                }
            }
        }
        this.sendQueueMode = Q_MODE_DESC;
        List<Message> msgTemp = this.sortByQueueMode(temp);
        return tryMessagesToConnections(msgTemp, connections);
    }

    protected List<Message> getMergedSortMessage() {
        List<Message> mergeMsg = new ArrayList<Message>(); // daftar pesan gabungan
        for (Connection c : getConnections()) { // untuk setiap koneksi yang tersedia
            DTNHost thisHost = getHost(); // this host

            DTNHost other = c.getOtherNode(thisHost); // peer
            EpidemicRouterFuzzy_Eli peer = (EpidemicRouterFuzzy_Eli) other.getRouter();
            // salinan pesan dari this host
            List<Message> messageBufferA = new ArrayList<>(getMessageCollection());
            // salinan pesan dari peer
            List<Message> messageBufferB = new ArrayList<>(peer.getMessageCollection());

            // gabungkan keduanya
            mergeMsg.addAll(messageBufferA);
            mergeMsg.addAll(messageBufferB);
        }
        // panggil callPriority untuk dapat prioritas pesan dlm daftar mergeMsg
        callPriority(mergeMsg);

        // mengurutkan pesan berdasarkan prioritas 
        Collections.sort(mergeMsg, new Comparator<Message>() {
            @Override
            public int compare(Message message1, Message message2) {
                double firstMessage = message1.getPriority();
                double secondMessage = message2.getPriority();

                if (firstMessage == secondMessage) {
                    return 0;
                } else if (firstMessage < secondMessage) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        return mergeMsg;
    }

    // jika sudah dikirim maka
    @Override
    public Message messageTransferred(String id, DTNHost from) {

        Message m = super.messageTransferred(id, from);
        if (m.getTo().equals(getHost())) {
            deleteAckedList();
            createAckedList(m, from);
        }

        return m;
    }

    @Override
    public void update() {
        super.update();

        exchangeAckedList();

        // apakah router sedang transfer atau tdk dpt memulai transfer
        if (isTransferring() || !canStartTransfer()) {
            deleteAckedList();
            return; // transferring, don't try other connections yet
        }

        // Try first the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }

        deleteAckedList();

        // then try any/all message to any/all connection
        this.tryAllMessagesToAllConnections();
    }

    @Override
    public EpidemicRouterFuzzy_Eli replicate() {
        return new EpidemicRouterFuzzy_Eli(this);
    }

}
