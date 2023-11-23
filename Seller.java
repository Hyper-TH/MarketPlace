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
    
    private volatile boolean isReceiving = true;
    private volatile boolean isSending = true;
    private boolean isRunning = true;

    private int receivingCounter = 0;
    private int broadcastTime = 0;
    private volatile boolean isBroadcasting = true;

    private ArrayList<Item> itemList;

    private static Scanner input = new Scanner(System.in);

    public int getCurrItem() {
        return currItem;
    }

    public void setCurrItem(int currItem) {
        this.currItem = currItem;
    }


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
        this.itemList.add(new Item(nodeID, "Potatoes", random.nextInt(20) + 1));
        this.itemList.add(new Item(nodeID, "Oil", random.nextInt(20) + 1));
        this.itemList.add(new Item(nodeID, "Flour", random.nextInt(20) + 1));
        this.itemList.add(new Item(nodeID, "Sugar", random.nextInt(20) + 1));

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
        System.out.println("1. Show All Items. \t\n2. Broadcast items on Sale. \t\n3. Current Item on Sale \t\n4. Buy an Item. \t\n5. Leave Market.");

        int choice = input.nextInt(); // Read user input

        switch (choice) {
            case 1:
                // Show all Items
                System.out.println("\n==== Current Items: =====\n");

                showAllItems(); // Show seller's current items

                break;
            case 2:
                // Broadcast Items on Sale 
                System.out.println("\n==== Broadcast Items: ====\n");   // This will only broadcast for 20 seconds

                sendItemsOnSale();  

                break;
            case 3:
                // Show current Item on sale
                Item currentItem = itemList.get(getCurrItem());

                System.out.println("Current item on sale: " + currentItem);

                break;
            case 4: 
                System.out.println("\n==== Buy an Item: ====\n");
                getItems();

                sendMessageToOtherSellers();
                getReceiptFromBuyer();

                break;
            case 5:
                System.out.println("\nLeaving Market... Goodbye.\n");
                System.exit(0);

                break;
            default:
                System.out.println("\nCould not read input. Please Try Again.\n");

                break;
        } // End switch
    }

    // This shows all the items that the seller has
    private void showAllItems() throws InterruptedException{
        for (Item item : itemList) {
            System.out.println(item);
        }
    }

    private void sendMessageToOtherSellers() {
        isSending = true;
        System.out.print("\nBuy Item (Press 'q' to Quit): ");

        while (isSending) {
            // Consume any extra newline characters
            input.nextLine();
            
            // Read the entire line
            String line = input.nextLine();
    
            // Split the line into parts (assuming space-separated values)
            String[] parts = line.split(" ");
    
            // If user chooses to exit
            if (parts.length >= 2 && parts[0].equals("q")) {
                System.out.println("Thank you for shopping with us!");
                System.exit(0);

                break;
            } 
            // User inputs <NODEID> <AMOUNT>
            else if (parts.length >= 2) {
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

    private void getReceiptFromBuyer() {
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
                    // If transaction was not successful (i.e., receives error message)
                    else if (message.contains("Requested amount")) {
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

            // Convert the message to bytes
            byte[] messageBytes = message.getBytes();

            // Create a DatagramPacket with the message and send it to the seller
            DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, sellerAddress, port);
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

    // Broadcast items
    private void sendItemsOnSale() {  
        setBroadcasting(true);

        int currentItemIndex = 0;

        // Listen while broadcasting
        new Thread(() -> {
            while (true) {
                try {
                    receiveMessages();  // This also receives messages from other sellers
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        
        // Note that every seller can only broadcast their items for 20 seconds max
        // Items are switched every 60 seconds
        while (isBroadcasting) {

            // Broadcast current items to buyers in the multicast group
            // Only send it if the amount is greater than 0
            if ((itemList.get(currentItemIndex)).getAmount() > 0) {
                
                // Show seller time left for broadcasting
                System.out.println("Broadcasting time left: " + (60 - broadcastTime));

                // Broadcast the item
                sendItems(itemList.get(currentItemIndex));

                try {
                    Thread.sleep(1000); // Sleep for 1 seconds before sending the next message
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // currItem is for update()
                setCurrItem(currentItemIndex);

            } 
            // If the amount of current item index is 0, move to the next item!
            else if ((itemList.get(currentItemIndex)).getAmount() == 0) {
                System.out.println("Current Item sold out, moving to the next item!");

                currentItemIndex = (currentItemIndex + 1) % itemList.size();
            }

            // Append broadcastTime
            broadcastTime += 1;

            // Once broadcast time reaches 60 (i.e., item has been sold for 60 seconds)
            if (broadcastTime == 61) {
                // This ensures that the currentIndex goes on a loop, iterating base of the size of the array
                currentItemIndex = (currentItemIndex + 1) % itemList.size();

                setBroadcastTime(0);
            } 
            // If broadcast time has reached 20 seconds/60 seconds
            else if (broadcastTime % 20 == 0 || broadcastTime == 60) {
                System.out.println("Broadcasting stopped!");

                setBroadcasting(false);

                break;
            }
        }
    }   // end sendItemsOnSale()

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

            String message = new String(inPacket.getData(), 0, inPacket.getLength());

            // If message is not a broadcasted item from other sellers 
            // and the message is intended for the seller
            // and the message is not a receipt
            if (!(message.contains("ProductName")) && extractNodeIDFromBuyer(message) && !(message.contains("bought"))) {

                // Stop the broadcast!
                setBroadcasting(false);

                // Update the items 
                String[] parts = message.split(" "); 
                String amount = parts[1];
                String buyerNodeID = parts[2];

                updateItems(Integer.parseInt(amount), buyerNodeID);

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

    private void sendErrorToBuyer() {
        try {
            String message =  "Requested amount from user is greater than our stock!";
            byte[] buffer = message.getBytes();
    
            outPacket = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(outPacket);
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendReceiptToBuyer(String item, int amount, String buyerNodeID){
        try {
            String message = buyerNodeID + " bought " + amount + " of " + item + " from " + nodeID;
            byte[] buffer = message.getBytes();
    
            outPacket = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(outPacket);
            
            System.out.println(message);

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
            sendErrorToBuyer();
        }
        else {

            // Subtract reduceAmount from the current amount
            int updatedAmount = currentAmount - reduceAmount;
        

            // Ensure the updated amount is not negative
            updatedAmount = Math.max(updatedAmount, 0);
        
            // Update the amount of the items
            itemToUpdate.setAmount(updatedAmount);


            // send Receipt to Seller
            sendReceiptToBuyer(itemToUpdate.getProductName(), reduceAmount, buyerNodeID);
        }

    }
    
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

    public static void main(String[] args) {
        new Thread(new Seller()).start();
    }
}
