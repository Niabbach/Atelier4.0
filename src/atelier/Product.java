package atelier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Product {
    String name;
    HashMap<Integer, List<ProductAction>> stages;
    public Product(String name, HashMap<Integer, List<ProductAction>> stages)
    {
        this.name = name;
        this.stages = stages;
    }
}
