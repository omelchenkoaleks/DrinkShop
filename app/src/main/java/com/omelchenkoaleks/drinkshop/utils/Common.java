package com.omelchenkoaleks.drinkshop.utils;

        import com.omelchenkoaleks.drinkshop.retrofit.IDrinkShopAPI;
        import com.omelchenkoaleks.drinkshop.retrofit.RetrofitClient;

public class Common {
    // In Emulator, localhost = 10.0.2.2
    private static final String BASE_URL = "http://10.0.2.2/drinkshop/";

    public static IDrinkShopAPI getAPI() {
        return RetrofitClient.getClient(BASE_URL).create(IDrinkShopAPI.class);
    }
}
