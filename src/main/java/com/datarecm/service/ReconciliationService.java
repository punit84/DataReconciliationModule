package com.datarecm.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.athena.model.GetQueryResultsRequest;
import com.amazonaws.services.athena.model.GetQueryResultsResult;
import com.amazonaws.services.athena.model.Row;
import com.amazonaws.util.CollectionUtils;
import com.datarecm.service.config.AppConfig;
import com.datarecm.service.config.DBConfig;

@Component
public class ReconciliationService {
	
	public static Log logger = LogFactory.getLog(ReconciliationService.class);
	@Autowired
	public S3AsyncOps s3Service;
	//public GlueService glueService;

	@Autowired
	SQLRunner sqlRunner ;

	@Autowired
	ReportingService report;

	int ruleIndexForMd5=4;
	int ruleIndexForUnmatchResult=6;
	int ruleIndexForMetadata=0;
	int ruleIndexForRecordCount=1;
	
	@Autowired
	private AppConfig appConfig;

	public File runRecTest(DBConfig sourceConfig, DBConfig targetConfig) throws Exception {
		ReportFileUtil fileUtil=null;
		long time=System.currentTimeMillis();

		try {
			
	
		String fileName = appConfig.getReportFile()+"-"+sourceConfig.getDbtype()+"-"+targetConfig.getDbtype()+".txt";
		fileUtil= new ReportFileUtil(fileName);
		AthenaService athenaService = new AthenaService();
		athenaService.setTarget(targetConfig);
		athenaService.setAppConfig(appConfig);
		sqlRunner.setSource(sourceConfig);
		report.setConfig(sourceConfig, targetConfig);
		logger.debug("************************");	
		int sourcerulecount=appConfig.getSourceRules().size();
		int destinationrulecount=appConfig.getTargetRules().size();
		if (sourcerulecount!=destinationrulecount) {
			logger.error("Rule count must be equal to run the report \n");	
			System.exit(0);
		}

		//Running all Athena Queries
		athenaService.submitAllQueriesAsync();

		fileUtil.createReportFile(sourceConfig, targetConfig);;
		runMetadataRules(fileUtil,athenaService);

		if (sourceConfig.isEvaluateDataRules()) {
			//build other queries
			buildRuleAndRunAthenaQuery(fileUtil,athenaService);

			runDataCount(fileUtil,athenaService);
			//run count rule
			runDataComparisionRules(sourceConfig, targetConfig,fileUtil,athenaService);
		}

		long timetaken = System.currentTimeMillis()-time;
		fileUtil.printEndOfReport(timetaken);
				
		} catch (Exception e) {
			long timetaken = System.currentTimeMillis()-time;
			fileUtil.printError(e, timetaken);
		}
		return fileUtil.getFile();

	}

	public String runRecTestURL(DBConfig sourceConfig, DBConfig targetConfig) throws Exception {
	
		File reportFile =runRecTest(sourceConfig, targetConfig);	
		String keyName = appConfig.getReportPath()+sourceConfig.getTableSchema()+"-"+sourceConfig.getTableName()+"_" +targetConfig.getDbname()+"-"+targetConfig.getTableName()+"-"+ (new Date()).toString();

		s3Service.uploadFile(appConfig.getS3bucket(),keyName , reportFile, targetConfig.getRegion());
		String url = s3Service.generateURL(appConfig.getS3bucket(), keyName);

		return url;
	}
	
	public void uploadToS3(DBConfig sourceConfig, DBConfig targetConfig, Map<String, String> sourceResult) throws Exception {
		
		String keyName = appConfig.getReportPath()+ AppConfig.MD5FILEPREFIX+ sourceConfig.getTableSchema()+"-"+sourceConfig.getTableName()+".txt";

		s3Service.uploadText(appConfig.getS3bucket(), keyName , sourceResult, targetConfig.getRegion());
		logger.info("\n\nSource result uploaded to s3 " +   keyName);

	}
//	
//	public File runRecTest() throws Exception {
//		return runRecTest(defaultConfig.source(),defaultConfig.target());		
//	}

	private void runMetadataRules(ReportFileUtil fileUtil,AthenaService athenaService)
			throws Exception {
		List<String> rules = appConfig.getSourceRules();
		//Map<Integer, Map<String, List<Object>>> sqlResutset= new HashMap<>();

		logger.info("\n*******************Executing Source Query :"+ ruleIndexForMetadata+" *************");

		String updatedSourceRule=rules.get(ruleIndexForMetadata);

		Map<String, List<String>> sourceResult = sqlRunner.executeSQL(ruleIndexForMetadata , updatedSourceRule);
		//sqlResutset.put(ruleIndex, sourceResult);

		Map<String, List<String>> destResult   = athenaService.getProcessedQueriesResultSync(ruleIndexForMetadata);
		logger.info("\n*******************Execution successfull *************");
		fileUtil.buildSchemaQueries(sourceResult,destResult);
		report.printMetadataRules(fileUtil);
	}

	private void runDataCount(ReportFileUtil fileUtil,AthenaService athenaService)
			throws InterruptedException, IOException {
		//Map<Integer, Map<String, List<Object>>> sqlResutset= new HashMap<>();

		logger.info("\n*******************Executing Source Query :"+ ruleIndexForRecordCount+" *************");

		String updatedSourceRule=appConfig.getSourceRules().get(ruleIndexForRecordCount).trim();

		Map<String, List<String>> sourceResult = sqlRunner.executeSQL(ruleIndexForRecordCount , updatedSourceRule);
		//sqlResutset.put(ruleIndex, sourceResult);
		Map<String, List<String>> destResult   = athenaService.getProcessedQueriesResultSync(ruleIndexForRecordCount);
		int sourceCount = Integer.parseInt(sourceResult.get("count").get(0));
		int destCount =Integer.parseInt(destResult.get("count").get(0));


		logger.info("\n*******************Execution successfull *************");
		report.printCountRules(ruleIndexForRecordCount,sourceCount,destCount, fileUtil);
	}

	private void buildRuleAndRunAthenaQuery(ReportFileUtil fileUtil,AthenaService athenaService) throws InterruptedException {
		report.buildMD5Queries(fileUtil);
		athenaService.submitQuery(ruleIndexForMd5 ,fileUtil.destSchema.getQuery());		
	}
	
	private void runDataComparisionRules(DBConfig sourceConfig, DBConfig targetConfig, ReportFileUtil fileUtil,AthenaService athenaService) throws Exception {
		Map<String, String> sourceResult = sqlRunner.executeSQLForMd5(ruleIndexForMd5 , fileUtil.sourceSchema.getQuery());
		
		
		GetQueryResultsRequest getQueryResultsRequest = athenaService.getQueriesResultSync(ruleIndexForMd5);
		logger.info("Comparing using md5,rowcount : "+sourceResult.size() );
		
//		CompletableFuture.runAsync(() -> {
//			try {
//				logger.info("Source file uploading to s3... ");
//
//				//uploadToS3(sourceConfig, targetConfig, sourceResult);
//				logger.info("Source file uploaded successfully to s3... ");
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		});

		
		List<String> unmatchIDs = report.compareRecData(ruleIndexForMd5, sourceResult, getQueryResultsRequest, fileUtil,athenaService);
		logger.info("Unmatched ids" + unmatchIDs);
		if (CollectionUtils.isNullOrEmpty(unmatchIDs)) {
			return;
		}
		report.buildUnmatchedResultQueries(unmatchIDs,fileUtil);
		athenaService.submitQuery(ruleIndexForUnmatchResult ,fileUtil.destSchema.getFetchUnmatchRecordQuery());		

		Map<String, List<String>> sourceUnmatchResult = sqlRunner.executeSQL(ruleIndexForUnmatchResult, fileUtil.sourceSchema.getFetchUnmatchRecordQuery());

		
		
		Map<String, List<String>> destUnmatchedResults = athenaService.getProcessedQueriesResultSync(ruleIndexForUnmatchResult);

		report.printUnmatchResult(sourceUnmatchResult, destUnmatchedResults, fileUtil);

	}
	


}