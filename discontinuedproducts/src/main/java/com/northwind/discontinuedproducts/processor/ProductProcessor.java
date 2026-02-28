package com.northwind.discontinuedproducts.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.northwind.discontinuedproducts.dto.DiscontinuedProduct;
import com.northwind.discontinuedproducts.dto.Product;

//transformation logic for the batch job, converting Product to DiscontinuedProduct
@Component
public class ProductProcessor implements ItemProcessor<Product, DiscontinuedProduct> {
    @Override
    public DiscontinuedProduct process(Product product) throws Exception {
        if(product.getDiscontinued()==null || product.getDiscontinued() != 1){
            return null;
        }
        DiscontinuedProduct discontinuedProduct = new DiscontinuedProduct();
        discontinuedProduct.setProductId(product.getProductId());   
        discontinuedProduct.setProductName(product.getProductName());
        discontinuedProduct.setLastKnownPrice(product.getUnitPrice());
        discontinuedProduct.setLastKnownStock(product.getUnitsInStock());
        discontinuedProduct.setArchivedAt(java.time.LocalDateTime.now());
        discontinuedProduct.setArchiveReason("DISCONTINUED");
        System.out.printf("Processing: [%s] -> Archived%n", product.getProductName());
        return discontinuedProduct;
        
    }
}
