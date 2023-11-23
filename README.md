# Compile application

Open a terminal in the folder.

This compiles all the files into a folder called `bin`.
```bash
javac -d ./bin Buyer.java Item.java Main.java Seller.java
```

# Run application

```bash
java -classpath ./bin Main
```

# Have multiple instances of Sellers and Buyers

Please open up a new terminal and run application again.


# To force close the application

If by chance the thread seems to have stopped working, force close the terminal by inputting `CTRL + C` or manually close the terminal. Then run it again.