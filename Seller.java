import java.net.*;
import java.io.*;
import java.util.*;

public class Seller implements Runnable {

    private int nodeID;
    private int currItem;
    private static MulticastSocket socket;
    private static InetAddress address;
    private static DatagramPacket inPacket, outPacket;
    private static byte[] buffer;
    private static int port = 7859;
    private int broadcastTime = 0;
    private volatile boolean isReceiving = true;
    private volatile boolean isSending = true;
    private boolean isRunning = true;

    private int receivingCounter = 0;
    private ArrayList<Item> itemList;
    private static Scanner input = new Scanner(System.in);


    public int getCurrItem() {
        return currItem;
    }

    public void setCurrItem(int currItem) {
        this.currItem = currItem;
    }

    private volatile boolean isBroadcasting = true;

    public boolean isBroadcasting() {
        return isBroadcasting;
    }

    public void setBroadcasting(boolean isBroadcasting) {
        this.isBroadcasting = isBroadcasting;
    }



    public int getBroadcastTime() {
        return broadcastTime;
    }

    public void setBroadcastTime(int broadcastTime) {
        this.broadcastTime = broadcastTime;
    }

    public Seller() {

        Random random = new Random();

        this.nodeID = random.nextInt(9000) + 1000;
        this.currItem = 0;
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

        while(true) {
            try {
                sellerMenu();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void sellerMenu() throws InterruptedException {
        // Menu Option for Seller
        System.out.println("==== Menu: =====\t");
        System.out.println("1. Show All Items. \t\n2. Broadcast items on Sale. \t\n3. But an Item. \t\n4. Leave Market.");

        int choice = input.nextInt(); // Read user input

        switch (choice) {
            case 1:
                // Show all Items
                System.out.println("\n==== Current Items: =====\n");
                showAllItems(); // Show seller's current items
                break;
            case 2:
                // Show Items on Sale 
                System.out.println("\n==== Broadcast Items: ====\n");   // This will only broadcast for 20 seconds

                sendItemsOnSale();  
                break;
            case 3: 
                System.out.println("\n==== Buy an Item: ====\n");
                getItems();

                sendMessageToOtherSellers();
                getReceipt();
                break;
            case 4:
                System.out.println("\nLeaving Market... Goodbye.\n");
                System.exit(0);
                break;
            default:
                System.out.println("\nCould not read input. Please Try Again.\n");
                break;
        } // End switch
    }

    private void showAllItems() throws InterruptedException{
        for (Item item : itemList) {
            System.out.println("NodeID: " + item.getNodeID() +
                    "| ProductID: " + item.getProductName() +
                    "| Amount: " + item.getAmount() + "\n");
        }
    }

    private void sendMessageToOtherSellers() {
        isSending = true;
    
        while (isSending) {
            System.out.print("\nBuy Item (Press 'q' to Quit): ");
    
            // Read the entire line
            String line = input.nextLine();
    
            // Split the line into parts (assuming space-separated values)
            String[] parts = line.split(" ");
    
            if (parts.length >= 2 && parts[0].equals("q")) {
                System.out.println("Thank you for shopping with us!");
                System.exit(0);
                break;
            } else if (parts.length >= 2) {
                // Assuming the first part is SellerNodeID and the second part is Amount
                String message = parts[0] + " " + parts[1] + " " + nodeID;
                
                // SellerNodeID Amount BuyerNodeID
                sendToSeller(message);
                isSending = false;
                break;
            } else {
                System.out.println("Invalid input. Please enter SellerNodeID and Amount separated by a space.");
            }
        }
    }

    private void getReceipt() {
        isReceiving = true;

        while (isReceiving) { 
            try {

                buffer = new byte[1024];
                inPacket = new DatagramPacket(buffer, buffer.length);

                socket.receive(inPacket);

                String message = new String(inPacket.getData(), 0, inPacket.getLength());
                System.out.println(message);
                synchronized (System.out) {
                    if (message.contains(Integer.toString(nodeID)) && (message.contains("bought"))) {
                        System.out.println(message);

                        isReceiving = false;
                        break;
                    } else if (message.contains("Requested amount")) {
                        System.out.println("Error making a transaction!");

                        isReceiving = false;
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    private void getItems() {
        isReceiving = true;
        receivingCounter = 0;

        while (isReceiving) { 
            try {

                buffer = new byte[1024];
                inPacket = new DatagramPacket(buffer, buffer.length);

                socket.receive(inPacket);

                String message = new String(inPacket.getData(), 0, inPacket.getLength());
                if (!message.contains(Integer.toString(nodeID))) {
                    System.out.println(message);

                    receivingCounter += 1;  
                }

                // Exit loop after receiving 5 items
                if (receivingCounter == 5) {

                    isReceiving = false;
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }   
    } // end getItems()

    private void sendItemsOnSale() {  
        setBroadcasting(true);
        int currentItemIndex = 0;

        // This is good, leave it as it is from now
        new Thread(() -> {
            while (true) {
                try {
                    receiveMessages();  // This also receives messages from other sellers
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        
        while (isBroadcasting) {

            // Broadcast current items to buyers in the multicast group
            // Only send it if the amount is greater than 0
            if ((itemList.get(currentItemIndex)).getAmount() > 0) {
                sendItems(itemList.get(currentItemIndex));
                try {
                    Thread.sleep(1000); // Sleep for 1 seconds before sending the next message
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // currItem is for update()
                setCurrItem(currentItemIndex);
            }
            
            broadcastTime += 1;

            if (broadcastTime == 61) {
                // This ensures that the currentIndex goes on a loop, iterating base of the size of the array
                currentItemIndex = (currentItemIndex + 1) % itemList.size();

                setBroadcastTime(0);
            } else if (broadcastTime % 20 == 0 || broadcastTime == 60) {
                System.out.println("Broadcasting stopped!");

                setBroadcasting(false);
                break;
            }
        }
    }   // end sendItemsOnSale()


    @Override
    public void run() {

        System.out.println("Seller nodeID: " + nodeID);
        while (isRunning) {
        
            setBroadcastTime(0);
            
        }
        // Optionally, close the socket when leaving the market
        if (!socket.isClosed()) {
            socket.close();
        }
    }

    // This is good, we will leave this alone for now
    private void sendItems(Item item) {
        try {
            String message = item + "\nNodeID of seller: " + nodeID;
            byte[] buffer = message.getBytes();
    
            outPacket = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(outPacket);
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private void receiveMessages() throws InterruptedException {
        try {
            buffer = new byte[1024];
            inPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(inPacket);

            // Extract nodeID from the received message
            String message = new String(inPacket.getData(), 0, inPacket.getLength());

            // If message is not a broadcasted item from other sellers 
            // and the message is intended for the seller
            if (!(message.contains("ProductName")) && extractNodeIDFromBuyer(message)) {

                // Stop the broadcast!
                setBroadcasting(false);

                // Update the items 
                String[] parts = message.split(" "); 
                String amount = parts[1];
                String buyerNodeID = parts[2];

                updateItems(Integer.parseInt(amount), buyerNodeID);

                System.out.println("Buyer bought something from you!");
            } 

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to check if the buyer wants to get from this specific seller
    private boolean extractNodeIDFromBuyer(String message) {
        String[] parts = message.split(" "); 
        String sellerNodeID = parts[0];

        if (Integer.parseInt(sellerNodeID) == nodeID) {
            return true;
        } else {
            return false;
        }
    }

    private void sendError(){
        try {
            String message =  "Requested amount from user is greater than our stock!";
            byte[] buffer = message.getBytes();
    
            outPacket = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(outPacket);
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendReceipt(String item, int amount, String buyerNodeID){
        try {
            String message = buyerNodeID + " bought " + amount + " of " + item + " from " + nodeID;
            byte[] buffer = message.getBytes();
    
            outPacket = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(outPacket);
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateItems(int reduceAmount, String buyerNodeID) {
        // Get the item to update
        Item itemToUpdate = itemList.get(getCurrItem());
    
        // Get the current amount of the item
        int currentAmount = itemToUpdate.getAmount();

        if(currentAmount < reduceAmount) {
            sendError();
        }
        else {

            // Subtract reduceAmount from the current amount
            int updatedAmount = currentAmount - reduceAmount;
        

            // Ensure the updated amount is not negative
            updatedAmount = Math.max(updatedAmount, 0);
        
            // Update the amount of the items
            itemToUpdate.setAmount(updatedAmount);

            System.out.println("Products have been updated!");

            // send Receipt to Seller
            sendReceipt(itemToUpdate.getProductName(), reduceAmount, buyerNodeID);
        }

    }
    public static void main(String[] args) {
        new Thread(new Seller()).start();
    }
}
