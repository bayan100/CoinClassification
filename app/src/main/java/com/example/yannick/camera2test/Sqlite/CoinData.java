package com.example.yannick.camera2test.Sqlite;

public class CoinData {
    // Value 0 = 2 Euro, 1 = 1 Euro, 2 = Golden Coins, 3 = Copper Coins
    public int value;
    public String country;

    public CoinData(int value, String country){
        this.value = value;
        this.country = country;
    }
}
