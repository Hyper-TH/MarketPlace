import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class Seller implements Runnable {

    private int nodeID;
    private int currItem;
    private static MulticastSocket socket;
    private static InetAddress address;
    private static DatagramPacket inPacket, outPacket;
    private static byte[] buffer;
    private static int port = 1099;

    private ArrayList<Item> itemList;

    public Seller() {

        Random random = new Random();
        this.nodeID = random.nextInt(9000) + 1000;
        this.currItem = 0;

        // Create a list of items for serialization
        this.itemList = new ArrayList<>();
        this.itemList.add(new Item(nodeID, "Potatoes", 5));
        this.itemList.add(new Item(nodeID, "Oil", 8));
        this.itemList.add(new Item(nodeID, "Flour", 12));
        this.itemList.add(new Item(nodeID, "Sugar", 13));

        try {
            address = InetAddress.getByName("224.0.0.3");
            socket = new MulticastSocket(port);
            socket.joinGroup(address);
        } catch (UnknownHostException uhe) {
            uhe.printStackTrace();
            System.exit(1);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.exit(2);
        }
    }

    @Override
    public void run() {


        // Listen for messages from buyers in a separate thread
        new Thread(() -> {
            while (true) {
                receiveMessagesFromBuyer();
            }
        }).start();

        int currentItemIndex = 0;

        while (true) {
            // Broadcast current items to buyers in the multicast group
            // Only send it if the amount is greater than 0
            if ((itemList.get(currentItemIndex)).getAmount() > 0) {
                sendItems(itemList.get(currentItemIndex));
                currItem = currentItemIndex;
            }
            try {
                Thread.sleep(10000); // Sleep for 5 seconds before sending the next message
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            currentItemIndex = (currentItemIndex + 1) % itemList.size();
        }
    }

    private void sendItems(Item item) {
        try {
            String message = "\nItems: \n" + item + "\nNodeID of seller: " + nodeID;
            byte[] buffer = message.getBytes();
    
            outPacket = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(outPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private void receiveMessagesFromBuyer() {
        try {
            buffer = new byte[1024];
            inPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(inPacket);

            // Extract nodeID from the received message
            String message = new String(inPacket.getData(), 0, inPacket.getLength());
            // int receivedNodeID = extractSellerNodeID(message);
            
            // System.out.println("Length of message: " + message.length());

            // Check if the sender's nodeID is the same as the seller's nodeID
            // This ensures it does not get its own list of items
            if (message.length() > 20) {

                // Print out other sellers' items
                // if (receivedNodeID != nodeID) {
                //     System.out.println("Received message from Buyer: " + message);
                // }
            } else if (message.length() < 10) {
                // Check if buyer wants to sell to this specific seller
                if (extractNodeIDFromBuyer(message)) {
                    System.out.println("Buyer bought something from you!");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int extractSellerNodeID(String message) {
        // Extract the nodeID from the message (you may need to adjust this based on your message format)
        String[] parts = message.split("\nNodeID of seller: ");
        if (parts.length > 1) {
            return Integer.parseInt(parts[1]);
        } else {
            return -1; // Handle the case where nodeID is not found
        }
    }

    private boolean extractNodeIDFromBuyer(String message) {
        String[] parts = message.split(" ");

        String receivedNodeID = parts[0];
        String amount = parts[1];

        if (Integer.parseInt(receivedNodeID) == nodeID) {
            updateItems(Integer.parseInt(amount));
            return true;
        } else {
            return false;
        }
    }

    private void updateItems(int reduceAmount) {
        // Get the item to update
        Item itemToUpdate = itemList.get(currItem);
    
        // Get the current amount of the item
        int currentAmount = itemToUpdate.getAmount();
    
        // Subtract reduceAmount from the current amount
        int updatedAmount = currentAmount - reduceAmount;
    
        // Ensure the updated amount is not negative
        updatedAmount = Math.max(updatedAmount, 0);
    
        // Update the amount of the item
        itemToUpdate.setAmount(updatedAmount);
    
        // Print the updated itemList
        System.out.println("Updated items after purchase:");
        for (Item item : itemList) {
            System.out.println("NodeID: " + item.getNodeID() +
                    "| ProductID: " + item.getProductName() +
                    "| Amount: " + item.getAmount());
        }
    }
    public static void main(String[] args) {
        new Thread(new Seller()).start();
    }
}

