package com.example.batch;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableBatchProcessing
@Slf4j
public class BatchConfiguration {

    @Bean
    DefaultBatchConfigurer batchConfigurer() {
        return new DefaultBatchConfigurer() {
            private JobRepository jobRepository;
            private JobExplorer jobExplorer;
            private JobLauncher jobLauncher;

            @PostConstruct
            private void init() throws Exception {
                MapJobRepositoryFactoryBean jobRepositoryFactory = new MapJobRepositoryFactoryBean();
                jobRepository = jobRepositoryFactory.getObject();
                MapJobExplorerFactoryBean jobExplorerFactory = new MapJobExplorerFactoryBean(jobRepositoryFactory);
                jobExplorer = jobExplorerFactory.getObject();
                SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
                simpleJobLauncher.setJobRepository(jobRepository);
                simpleJobLauncher.afterPropertiesSet();
                jobLauncher = simpleJobLauncher;
            }

            @Override
            public JobRepository getJobRepository() {
                return jobRepository;
            }

            @Override
            public JobExplorer getJobExplorer() {
                return jobExplorer;
            }

            @Override
            public JobLauncher getJobLauncher() {
                return jobLauncher;
            }
        };
    }

    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    @Value("${chunk.csv2tsv:1}")
    private int chunkCsv2Tsv;

    @Value("${chunk.import:1}")
    private int chunkImport;

    @Bean
    public Job jobConvertPersonCsv2Tsv(Step stepConvertPersonCsv2Tsv) throws IOException {
        return jobBuilderFactory.get("jobConvertPersonCsv2Tsv")
                .incrementer(new RunIdIncrementer())
                .flow(stepConvertPersonCsv2Tsv)
                .end()
                .build();
    }

    @Bean
    public Job jobImportPerson(JobCompletionNotificationListener listener, Step stepImportPerson) {
        return jobBuilderFactory.get("jobImportPerson")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(stepImportPerson)
                .end()
                .build();
    }

    @Bean
    public Step stepConvertPersonCsv2Tsv(FlatFileItemReader<Person> personCsvReader,
            FlatFileItemWriter<Person> personTsvWriter) throws IOException {
        return stepBuilderFactory.get("stepConvertPersonCsv2Tsv")
                .<Person, Person>chunk(chunkCsv2Tsv)
                .reader(personCsvReader)
                .writer(personTsvWriter)
                .build();
    }

    @Bean
    public Step stepImportPerson(FlatFileItemReader<Person> personCsvReader, JdbcBatchItemWriter<Person> writer) {
        return stepBuilderFactory.get("step1")
                .<Person, Person>chunk(chunkImport)
                .reader(personCsvReader)
                .processor(personItemProcessor())
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Person> personCsvReader(@Value("#{jobParameters[csv]}") String filePath) {
        return new FlatFileItemReaderBuilder<Person>()
                .name("personItemReader")
                .resource(new FileSystemResource(filePath))
                .delimited()
                .names(new String[] { "firstName", "lastName" })
                .fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {
                    {
                        setTargetType(Person.class);
                    }
                })
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<Person> personTsvWriter(@Value("#{jobParameters[tsv]}") String filePath) {
        FlatFileItemWriter<Person> writer = new FlatFileItemWriter<>();
        writer.setResource(new FileSystemResource(filePath));
        writer.setLineAggregator(person -> {
            String tsv = String.join("\t", person.getFirstName(), person.getLastName());
            log.debug(tsv);
            return tsv;
        });
        return writer;
    }

    @Bean
    public PersonItemProcessor personItemProcessor() {
        return new PersonItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Person> personImportWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Person>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)")
                .dataSource(dataSource)
                .build();
    }

}
