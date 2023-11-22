import java.io.*;
import java.net.*;
import java.util.*;

public class Buyer implements Runnable {

    // This nodeID will be needed, for the receipt 
    // i.e., Seller will broadcast the receipt, check the receipt if it contains this nodeID, then get it
    private int nodeID;

    private static MulticastSocket socket;
    private static InetAddress address;
    private static DatagramPacket inPacket, outPacket;
    private static byte[] buffer;
    private static int port = 7859;

    private volatile boolean isRunning = true;
    private volatile boolean isReceiving = true;
    private volatile boolean isSending = true;
    private int receivingCounter = 0;

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }


    // Shared Scanner for user input
    private static Scanner input = new Scanner(System.in);

    // Probably don't need this, Buyer doesn't need a shopping basket, they can use their arms for carrying bought items
    // private ArrayList<Item> buyers = new ArrayList<>();

    public Buyer() {

        Random random = new Random();
        this.nodeID = random.nextInt(9000) + 1000;

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

        // Menu Option for Buyer
        System.out.println("==== Menu: =====\t");
        System.out.println("1. Join Market.\t\n2. Leave Market.");

        int choice = input.nextInt(); // Read user input

        switch (choice) {
            case 1:
                // Join Market
                System.out.println("\n==== Joined Market ====\n");
                // new Thread(() -> receiveTimeAndItemsFromSellers()).start();
                displayBuyerMenu();
                break;
            case 2:
                System.out.println("\nLeaving Market... Goodbye.\n");

                setRunning(false);
                break;
            default:
                System.out.println("\nCould not read input. Please Try Again.\n");
                break;
        } // End switch

    }

    private void displayBuyerMenu() {
        // Buyer menu options
        while (isRunning) {
            System.out.println("==== Buyer Menu: ====");
            System.out.println("1. Display current items on sale.");
            System.out.println("2. Buy an item.");
            System.out.println("3. Leave Market.");

            int choice = input.nextInt(); // Read user input

            switch (choice) {
                case 1:
                    // Display current items on sale
                    System.out.println("Getting items for sale now from buyers....");
                    getItems(); // Infinite loop
                    break;
                case 2:
                    System.out.println("Getting items for sale now from buyers....");
                    getItems(); // Have it so that it only runs for 10 seconds (i.e., listen for 10 seconds)

                    sendMessageToSeller();
                    getReceipt();
                    break;  
                case 3:
                    // Leave Market
                    System.out.println("\nLeaving Market... Goodbye.\n");
                    
                    setRunning(false);
                    System.exit(0);
                    break;
                default:
                    System.out.println("\nCould not read input. Please Tryd Again.\n");
                    break;
            }
        }
    }

    // Another bug might be produced here:
    // When other buyers are trying to buy, this buyer might see other buyer messages as well
    private void getItems() {
        isReceiving = true;
        receivingCounter = 0;

        while (isReceiving) { 
            try {

                buffer = new byte[1024];
                inPacket = new DatagramPacket(buffer, buffer.length);

                socket.receive(inPacket);

                String message = new String(inPacket.getData(), 0, inPacket.getLength());

                // Have an if statement logic here that checks if this is a buyer message or not
                // I.e., check message length (similar to the if statements in receiveMessages in seller)
                if (message.contains("ProductName")) {
                    System.out.println(message);

                    receivingCounter += 1;
                }
                // Exit loop after receiving two items
                if (receivingCounter == 2) {
                    System.out.println("Counter: " + receivingCounter);

                    isReceiving = false;
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }   
    } // end getItems()

    // Check if it's from other buyers
    private void getReceipt() {
        isReceiving = true;

        while (isReceiving) { 
            try {

                buffer = new byte[1024];
                inPacket = new DatagramPacket(buffer, buffer.length);

                socket.receive(inPacket);

                String message = new String(inPacket.getData(), 0, inPacket.getLength());

                synchronized (System.out) {
                    if (message.contains(Integer.toString(nodeID)) && (message.contains("bought"))) {
                        System.out.println(message);

                        isReceiving = false;
                        break;
                    } 
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }   
    }

    private void sendMessageToSeller() {
        
        try (Scanner input = new Scanner(System.in)) {
            isSending = true;

            while (isSending) {

                System.out.print("\nBuy Item (Press 'q' to Quit): ");
                String message = input.nextLine();

                if(message.equals("q")) {
                    System.out.println("Thank you for shopping with us!");
                    System.exit(0);
                    break;
                } else {
                    // SellerNodeID Amount BuyerNodeID
                    sendToSeller(message + " " + nodeID);
                    isSending = false;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendToSeller(String message) {
        try {
            // Specify the seller's address and port
            InetAddress sellerAddress = InetAddress.getByName("224.0.0.3");
            int sellerPort = 7859;

            // Convert the message to bytes
            byte[] messageBytes = message.getBytes();

            // Create a DatagramPacket with the message and send it to the seller
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, sellerAddress, sellerPort);
            socket.send(packet);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Buyer nodeID: " + nodeID);

        while (isRunning) {
                

        }
        // Optionally, close the socket when leaving the market
        if (!socket.isClosed()) {
            socket.close();
        }
    }


    public static void main(String[] args) {
        new Thread(new Buyer()).start();
    } // End of main
}