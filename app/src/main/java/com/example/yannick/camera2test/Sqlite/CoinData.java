package com.example.yannick.camera2test.Sqlite;

public class CoinData {
    // Value 0 = 2 Euro, 1 = Golden Coins, 2 = Copper Coins
    public int value;
    public String country;

    public CoinData(int value, String country){
        this.value = value;
        this.country = country;
    }
}
