class Item {
    private static final long serialVersionUID = 42L;

    private int nodeID;
    private String productName;
    private int amount;


    public Item(int nodeID, String productName, int amount) {
        this.nodeID = nodeID;
        this.productName = productName;
        this.amount = amount;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public int getNodeID() {
        return nodeID;
    }

    public void setNodeID(int nodeID) {
        this.nodeID = nodeID;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "NodeID: " + nodeID + " ProductName: " + productName + ", Amount: " + amount;
    }
}
