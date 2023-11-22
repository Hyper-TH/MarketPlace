import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Buyer implements Runnable {

    private int nodeID;

    private static MulticastSocket socket;
    private static InetAddress address;
    private static DatagramPacket inPacket, outPacket;
    private static byte[] buffer;
    private static int port = 1099;

    private volatile boolean isRunning = true;

    // Shared Scanner for user input
    private static Scanner input = new Scanner(System.in);

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
                isRunning = false; // Stop the main loop
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
                    receiveItems();
                    System.out.println("Please choose this option again to see other possible items!");

                    break;
                case 2:
                    System.out.println("Getting items for sale now from buyers....");
                    receiveItems();

                    // Buy an item
                    sendMessageToSeller();

                    break;  
                case 3:
                    // Leave Market
                    System.out.println("\nLeaving Market... Goodbye.\n");
                    isRunning = false; // Stop the main loop
                    break;
                default:
                    System.out.println("\nCould not read input. Please Try Again.\n");
                    break;
            }
        }
    }

    private void receiveItems() {
        try {
            buffer = new byte[1024];
            inPacket = new DatagramPacket(buffer, buffer.length);

            socket.receive(inPacket);

            String message = new String(inPacket.getData(), 0, inPacket.getLength());
            System.out.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToSeller() {
        try (Scanner input = new Scanner(System.in)) {
            while (isRunning) {
                System.out.print("Enter message to seller (type 'exit' to leave): ");
                String message = input.nextLine();

                if (message.equals("exit")) {
                    System.out.println("Leaving market... Goodbye.");
                    isRunning = false;
                    break;
                }

                // Send the user-input message to the seller
                sendToSeller(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendToSeller(String message) {
        try {
            // Specify the seller's address and port
            InetAddress sellerAddress = InetAddress.getByName("224.0.0.3");
            int sellerPort = 1099;

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
        // Additional logic for continuous operation
        while (isRunning) {
            // You can add more logic here if needed
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
