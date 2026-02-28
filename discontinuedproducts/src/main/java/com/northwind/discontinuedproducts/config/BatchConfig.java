package com.northwind.discontinuedproducts.config;
//job, step,reader,processor, writer 

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.northwind.discontinuedproducts.dto.DiscontinuedProduct;
import com.northwind.discontinuedproducts.dto.Product;
import com.northwind.discontinuedproducts.listener.JobCompletionListener;
import com.northwind.discontinuedproducts.processor.ProductProcessor;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
    //read from db and map to Product objects, using JdbcCursorItemReader for efficient streaming of large datasets
    @Bean
    public JdbcCursorItemReader<Product> productReader(DataSource dataSource) {
      return new JdbcCursorItemReaderBuilder<Product>()
          .name("productReader")
          .dataSource(dataSource)
          .sql("SELECT product_id, product_name, supplier_id, category_id, quantity_per_unit, unit_price, units_in_stock, units_on_order, reorder_level, discontinued FROM products WHERE discontinued = 1 ORDER BY product_id")
          .rowMapper((rs, rowNum) -> {
              Product product = new Product();
              product.setProductId(rs.getInt("product_id"));
              product.setProductName(rs.getString("product_name"));
              product.setSupplierId(rs.getInt("supplier_id"));
              product.setCategoryId(rs.getInt("category_id"));
              product.setQuantityPerUnit(rs.getString("quantity_per_unit"));
              product.setUnitPrice(rs.getDouble("unit_price"));
              product.setUnitsInStock(rs.getInt("units_in_stock"));
              product.setUnitsOnOrder(rs.getInt("units_on_order"));
              product.setReorderLevel(rs.getInt("reorder_level"));
              int discontinuedValue = rs.getInt("discontinued");
              if (rs.wasNull()) {
                  product.setDiscontinued(null);
              } else {
                  product.setDiscontinued(discontinuedValue);
              }
              return product;
          })
          .build();
    }
    //write to a log fie
    @Bean
    public FlatFileItemWriter<DiscontinuedProduct> fileWriter(){
        return new FlatFileItemWriterBuilder<DiscontinuedProduct>()
            .name("fileWriter")
            .resource(new FileSystemResource("discontinued_products.log"))
            .append(false)
            .lineAggregator(item -> 
                String.format("%d, %s, %.2f, %d, %s, %s", 
                    item.getProductId(), 
                    item.getProductName(), 
                    item.getLastKnownPrice(), 
                    item.getLastKnownStock(), 
                    item.getArchivedAt().toString(), 
                    item.getArchiveReason()
                )
            )
            .build();
    }
    //db writer 
    @Bean
    public JdbcBatchItemWriter<DiscontinuedProduct> dbWriter(DataSource dataSource){
       return new JdbcBatchItemWriterBuilder<DiscontinuedProduct>()
            .dataSource(dataSource)
            .sql("INSERT INTO discontinued_products (product_id, product_name, last_known_price, last_known_stock, archived_at, archive_reason) VALUES (:productId, :productName, :lastKnownPrice, :lastKnownStock, :archivedAt, :archiveReason)")
            .beanMapped()
            .build();
    }
    //composite writer to write to both file and db
    @Bean
    public CompositeItemWriter<DiscontinuedProduct> compositeWriter(
        FlatFileItemWriter<DiscontinuedProduct> fileWriter, 
        JdbcBatchItemWriter<DiscontinuedProduct> dbWriter){
        CompositeItemWriter<DiscontinuedProduct> compositeItemWriter = new CompositeItemWriter<>();
        compositeItemWriter.setDelegates(java.util.Arrays.asList(fileWriter, dbWriter));
        return compositeItemWriter;
    }

    //Assemble the beans into a cohesive batch job configuration, defining the job, steps, and linking the reader, processor, and writer together.
    @Bean
    public Step discontinuedProdStep(
        JobRepository jobRepository,
        PlatformTransactionManager txManager,
        JdbcCursorItemReader<Product> productReader,
        ProductProcessor productProcessor,
        CompositeItemWriter<DiscontinuedProduct> compositeWriter
    ){
        return new StepBuilder("discontinuedProdStep", jobRepository)
        //read process and write in chunks of 5 then commit as a batch, with fault tolerance to skip up to 3 exceptions without failing the entire job
            .<Product, DiscontinuedProduct>chunk(5, txManager)
            .reader(productReader)
            .processor(productProcessor)
            .writer(compositeWriter)
            .faultTolerant()
            .skip(Exception.class)
            .skipLimit(3)
            .build();
 
    }
    @Bean
    public Job discontinuedProductJob(
        JobRepository jobRepository,
        Step discontinuedProdStep,
        JobCompletionListener listener){
        return new JobBuilder("discontinuedProductJob", jobRepository)
            .listener(listener)
            .start(discontinuedProdStep)
            .build();
    }
    
}
