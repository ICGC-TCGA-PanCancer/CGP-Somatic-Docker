package io.seqware.pancancer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import net.sourceforge.seqware.pipeline.workflowV2.AbstractWorkflowDataModel;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;
import org.apache.commons.lang.StringUtils;
import io.seqware.pancancer.Version;
import java.util.UUID;

/**
 * <p>For more information on developing workflows, see the documentation at
 * <a href="http://seqware.github.io/docs/6-pipeline/java-workflows/">SeqWare Java Workflows</a>.</p>
 *
 * Quick reference for the order of methods called:
 * 1. setupDirectory
 * 2. setupFiles
 * 3. setupWorkflow
 * 4. setupEnvironment
 * 5. buildWorkflow
 *
 * See the SeqWare API for
 * <a href="http://seqware.github.io/javadoc/stable/apidocs/net/sourceforge/seqware/pipeline/workflowV2/AbstractWorkflowDataModel.html#setupDirectory%28%29">AbstractWorkflowDataModel</a>
 * for more information.
 */
public class CgpCnIndelSnvStrWorkflow extends AbstractWorkflowDataModel {

  private static String OUTDIR = "outdir";
  private static String TIMEDIR;
  private static String COUNTDIR;
  private static String BBDIR;
  private boolean testMode=false;
  private boolean localFileMode=false;
  private boolean cleanup = false;
  private boolean cleanupBams = false;
  
  // datetime all upload files will be named with
  DateFormat df = new SimpleDateFormat("yyyyMMdd");
  String dateString = df.format(Calendar.getInstance().getTime());

  private String workflowName = Version.WORKFLOW_SHORT_NAME_VERSION;
  
  // MEMORY variables //
  private String  memBasFileGet, memGnosDownload, memPackageResults, memMarkTime,
                  memQcMetrics, memUpload, memGetTbi, memGenotype, memContam,
                  memPicnicCounts, memPicnicMerge, memUnpack, memBbMerge,
                  // ascat memory
                  memAlleleCount, memAscat, memAscatFinalise,
                  // pindel memory
                  memPindelInput, memPindelPerThread, memPindelVcf, memPindelMerge , memPindelFlag,
                  // brass memory
                  memBrassInput, memBrassGroup, memBrassFilter, memBrassSplit,
                  memBrassAssemblePerThread, memBrassGrass, memBrassTabix,
                  // caveman memory
                  memCaveCnPrep,
                  memCavemanSetup, memCavemanSplit, memCavemanSplitConcat,
                  memCavemanMstepPerThread, memCavemanMerge, memCavemanEstepPerThread,
                  memCavemanMergeResults, memCavemanAddIds, memCavemanFlag,
                  memCavemanTbiClean,
                  //upload BAM optionally
                  bamUploadServer, bamUploadPemFile
          ;

  // workflow variables
  private String  // reference variables
                  species, assembly,
                  // sequencing type/protocol
                  seqType, seqProtocol,
                  //GNOS identifiers
                  pemFile, uploadPemFile, gnosServer, uploadServer,
                  // ascat variables
                  gender,
                  // pindel variables
                  refExclude, pindelGermline,
                  //caveman variables
                  tabixSrvUri,
                  //general variables
                  installBase, refBase, genomeFaGz, testBase,
                  //contamination variables
                  contamDownSampOneIn
                  ;
  
  // variables related to upload 
  private boolean saveUploadArchive = false;
  private boolean S3UploadArchive = false;
  private boolean SFTPUploadFiles = false;
  private boolean S3UploadFiles = false;
  private boolean SFTPUploadArchive = false;
  private boolean SynapseUpload = false;

  // re-upload Bam
  private String bamUploadStudyRefnameOverride, bamUploadAnalysisCenterOverride, controlAnalysisId, bamUploadScriptJobMem, bamUploadScriptJobSlots;
  
  private String 
          uploadArchivePath,
          SFTPUploadArchiveUsername, SFTPUploadArchivePassword, SFTPUploadArchivePath, SFTPUploadArchiveServer,
          S3UploadArchiveBucketURL, S3UploadArchiveKey, S3UploadArchiveSecretKey,
          SFTPUploadUsername, SFTPUploadPassword, SFTPUploadPath, SFTPUploadServer,
          S3UploadBucketURL, S3UploadKey, S3UploadSecretKey, SFTPUploadArchiveMode, SFTPUploadMode,
          S3UploadArchiveMode, S3UploadFileMode;
  
  // variables related to tracking cloud environment
  private String vmInstanceType, vmInstanceCores, vmInstanceMemGb, vmLocationCode;

  private int coresAddressable, memWorkflowOverhead, memHostMbAvailable;
  
  // synapse upload variables
  private String SynapseUploadSFTPUsername, SynapseUploadSFTPPassword, 
          SynapseUploadUsername, SynapseUploadPassword, SynapseUploadURL,
          SynapseUploadParent;
  
  private String duckJobMem;

  // UUID
  private String uuid = UUID.randomUUID().toString().toLowerCase();


  private void init() {
    try {
      //optional properties
      String outDir = OUTDIR;
      String outPrefix = "";
      if (hasPropertyAndNotNull("output_dir")) {
        outDir = getProperty("output_dir");
      }
      if (hasPropertyAndNotNull("output_prefix")) {
        outPrefix = getProperty("output_prefix");
      }
      if (!"".equals(outPrefix)) {
        if (outPrefix.endsWith("/")) {
          OUTDIR = outPrefix+outDir;
        } else {
          OUTDIR = outPrefix + "/" + outDir;
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    TIMEDIR = OUTDIR + "/timings";
    COUNTDIR = OUTDIR + "/ngsCounts";
    BBDIR = OUTDIR + "/bbCounts";
  }

  @Override
  public void setupDirectory() {
    //since setupDirectory is the first method run, we use it to initialize variables too.
    init();
    // creates a dir1 directory in the current working directory where the workflow runs
    addDirectory(OUTDIR);
    addDirectory(TIMEDIR);
    addDirectory(COUNTDIR);
    addDirectory(BBDIR);
  }

  @Override
  public Map<String, SqwFile> setupFiles() {
    try {
      
      if(hasPropertyAndNotNull("saveUploadArchive")) {
        saveUploadArchive=Boolean.valueOf(getProperty("saveUploadArchive"));
      }
      
      if(hasPropertyAndNotNull("S3UploadArchive")) {
        S3UploadArchive=Boolean.valueOf(getProperty("S3UploadArchive"));
      }
      
      if(hasPropertyAndNotNull("SFTPUploadFiles")) {
        SFTPUploadFiles=Boolean.valueOf(getProperty("SFTPUploadFiles"));
      }
      
      if(hasPropertyAndNotNull("S3UploadFiles")) {
        S3UploadFiles=Boolean.valueOf(getProperty("S3UploadFiles"));
      }
      
      if(hasPropertyAndNotNull("SFTPUploadArchive")) {
        SFTPUploadArchive=Boolean.valueOf(getProperty("SFTPUploadArchive"));
      }
      
      if(hasPropertyAndNotNull("SynapseUpload")) {
        SynapseUpload=Boolean.valueOf(getProperty("SynapseUpload"));
      }
      
      if(hasPropertyAndNotNull("cleanup")) {
        cleanup = Boolean.valueOf(getProperty("cleanup"));
      }
      
      if(hasPropertyAndNotNull("cleanupBams")) {
        cleanupBams = Boolean.valueOf(getProperty("cleanupBams"));
      }
      
      if(hasPropertyAndNotNull("testMode")) {
        testMode=Boolean.valueOf(getProperty("testMode"));
        if (testMode) {
          System.err.println("WARNING\n\tRunning in test mode, direct access BAM files will be used, change 'testMode' in ini file to disable\n");
        }
      }

      if(hasPropertyAndNotNull("localFileMode")) {
        localFileMode=Boolean.valueOf(getProperty("localFileMode"));
        if (localFileMode) {
          System.err.println("WARNING\n\tRunning in direct file mode, direct access BAM files will be used and assumed to be full paths but metadata will still be downloaded from GNOS, change 'localFileMode' in ini file to disable\n");
        }
      }
      
      if(hasPropertyAndNotNull("uploadServer")) {
        uploadServer = getProperty("uploadServer");
      }
      else {
        System.err.println("WARNING\n\t'uploadServer' not defined in workflow.ini, no VCF upload will be attempted\n");
      }
      
      if(testMode && uploadServer != null) {
        System.err.println("WARNING\n\t'uploadServer' has been cleared as testMode is in effect\n");
        uploadServer = null;
      }
      
      // used by steps that can use all available cores
      coresAddressable = Integer.valueOf(getProperty("coresAddressable"));

      // MEMORY //
      memBasFileGet = getProperty("memBasFileGet");
      memGnosDownload = getProperty("memGnosDownload");
      memPackageResults = getProperty("memPackageResults");
      memMarkTime = getProperty("memMarkTime");
      memQcMetrics = getProperty("memQcMetrics");
      memGenotype = getProperty("memGenotype");
      memContam = getProperty("memContam");
      memUpload = getProperty("memUpload");
      memGetTbi = getProperty("memGetTbi");
      
      // upload
      bamUploadServer = getProperty("bamUploadServer");
      bamUploadPemFile = getProperty("bamUploadPemFile");
      
      memPicnicCounts = getProperty("memPicnicCounts");
      memPicnicMerge = getProperty("memPicnicMerge");
      memUnpack = getProperty("memUnpack");
      memBbMerge = getProperty("memBbMerge");
      
      memAlleleCount = getProperty("memAlleleCount");
      memAscat = getProperty("memAscat");
      memAscatFinalise = getProperty("memAscatFinalise");
      
      memPindelInput = getProperty("memPindelInput");
      memPindelPerThread = getProperty("memPindelPerThread");
      memPindelVcf = getProperty("memPindelVcf");
      memPindelMerge = getProperty("memPindelMerge");
      memPindelFlag = getProperty("memPindelFlag");
      
      memBrassInput = getProperty("memBrassInput");
      memBrassGroup = getProperty("memBrassGroup");
      memBrassFilter = getProperty("memBrassFilter");
      memBrassSplit = getProperty("memBrassSplit");
      memBrassAssemblePerThread = getProperty("memBrassAssemblePerThread");
      memBrassGrass = getProperty("memBrassGrass");
      memBrassTabix = getProperty("memBrassTabix");
      
      memCaveCnPrep = getProperty("memCaveCnPrep");
      memCavemanSetup = getProperty("memCavemanSetup");
      memCavemanSplit = getProperty("memCavemanSplit");
      memCavemanSplitConcat = getProperty("memCavemanSplitConcat");
      memCavemanMstepPerThread = getProperty("memCavemanMstepPerThread");
      memCavemanMerge = getProperty("memCavemanMerge");
      memCavemanEstepPerThread = getProperty("memCavemanEstepPerThread");
      memCavemanMergeResults = getProperty("memCavemanMergeResults");
      memCavemanAddIds = getProperty("memCavemanAddIds");
      memCavemanFlag = getProperty("memCavemanFlag");
      memCavemanTbiClean = getProperty("memCavemanTbiClean");
      
      memWorkflowOverhead = Integer.valueOf(getProperty("memWorkflowOverhead"));
      memHostMbAvailable = Integer.valueOf(getProperty("memHostMbAvailable"));
      
      contamDownSampOneIn = getProperty("contamDownSampOneIn");

      // REFERENCE INFO //
      species = getProperty("species");
      assembly = getProperty("assembly");
      
      // Sequencing info
      seqType = getProperty("seqType");
      if(seqType.equals("WGS")) {
        seqProtocol = "genomic";
      }

      // Specific to ASCAT workflow //
      gender = getProperty("gender");
      
      // Specific to Caveman workflow //
      tabixSrvUri = getProperty("tabixSrvUri");

      // pindel specific
      refExclude = getProperty("refExclude");

      // used for upload too so always get it
      if(hasPropertyAndNotNull("gnosServer")) {
        gnosServer = getProperty("gnosServer");
      }      
      
      // test mode
      if(!testMode || (hasPropertyAndNotNull("upload-test") && Boolean.valueOf(getProperty("upload-test")))) {
        pemFile = getProperty("pemFile");
        uploadPemFile = getProperty("uploadPemFile");
      }

      //environment
      installBase = getWorkflowBaseDir() + "/bin/opt";
      //refBase = getWorkflowBaseDir() + "/data/cgp_reference";
      refBase = getWorkflowBaseDir() + "/data/reference/cgp_reference";
      testBase = getWorkflowBaseDir() + "/data/testdata";
      genomeFaGz = getWorkflowBaseDir() + "/data/reference/genome.fa.gz";
      
      // variables for upload
      uploadArchivePath = getProperty("uploadArchivePath");
      SFTPUploadArchiveUsername = getProperty("SFTPUploadArchiveUsername");
      SFTPUploadArchivePassword = getProperty("SFTPUploadArchivePassword");
      SFTPUploadArchivePath = getProperty("SFTPUploadArchivePath");
      if (!SFTPUploadArchivePath.endsWith("/")) { SFTPUploadArchivePath = SFTPUploadArchivePath + "/"; }
      SFTPUploadArchiveServer = getProperty("SFTPUploadArchiveServer");
      S3UploadArchiveBucketURL = getProperty("S3UploadArchiveBucketURL");
      if (!S3UploadArchiveBucketURL.endsWith("/")) { S3UploadArchiveBucketURL = S3UploadArchiveBucketURL + "/"; }
      S3UploadArchiveKey = getProperty("S3UploadArchiveKey");
      S3UploadArchiveSecretKey = getProperty("S3UploadArchiveSecretKey");
      SFTPUploadUsername = getProperty("SFTPUploadUsername");
      SFTPUploadPassword = getProperty("SFTPUploadPassword");
      SFTPUploadPath = getProperty("SFTPUploadPath");
      if (!SFTPUploadPath.endsWith("/")) { SFTPUploadPath = SFTPUploadPath + "/"; }
      SFTPUploadServer = getProperty("SFTPUploadServer");
      S3UploadBucketURL = getProperty("S3UploadBucketURL");
      if (!S3UploadBucketURL.endsWith("/")) { S3UploadBucketURL = S3UploadBucketURL + "/"; }
      S3UploadKey = getProperty("S3UploadKey");
      S3UploadSecretKey = getProperty("S3UploadSecretKey");
      SFTPUploadMode = getProperty("SFTPUploadMode");
      SFTPUploadArchiveMode = getProperty("SFTPUploadArchiveMode");
      S3UploadFileMode = getProperty("S3UploadFileMode");
      S3UploadArchiveMode = getProperty("S3UploadArchiveMode");

      // tracking cloud
      vmInstanceType = getProperty("vm_instance_type");
      vmInstanceCores = getProperty("vm_instance_cores");
      vmInstanceMemGb = getProperty("vm_instance_mem_gb");
      vmLocationCode = getProperty("vm_location_code");

      // reupload bam
      bamUploadStudyRefnameOverride = getProperty("bamUploadStudyRefnameOverride");
      bamUploadAnalysisCenterOverride = getProperty("bamUploadAnalysisCenterOverride");
      controlAnalysisId = getProperty("controlAnalysisId");
      bamUploadScriptJobMem = getProperty("bamUploadScriptJobMem");
      bamUploadScriptJobSlots = getProperty("bamUploadScriptJobSlots");
      
      // synapse upload
      SynapseUploadSFTPUsername = getProperty("SynapseUploadSFTPUsername");
      SynapseUploadSFTPPassword = getProperty("SynapseUploadSFTPPassword");
      SynapseUploadUsername = getProperty("SynapseUploadUsername");
      SynapseUploadPassword = getProperty("SynapseUploadPassword");
      SynapseUploadURL = getProperty("SynapseUploadURL");
      SynapseUploadParent = getProperty("SynapseUploadParent");
      
      // upload 
      duckJobMem = getProperty("duckJobMem");
      
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    
    return getFiles();
  }
  
  private Job bamProvision(String analysisId, String bamFile, Job startTiming) {
    Job basJob = null;
    if(testMode == false) {

      Job gnosDownload = null;

      if (localFileMode == false) {
        gnosDownload = gnosDownloadBaseJob(analysisId, bamFile);
        gnosDownload.setMaxMemory(memGnosDownload);
        gnosDownload.addParent(startTiming);
        // the file needs to end up in tumourBam/controlBam
        // get the BAS files
        basJob = basFileBaseJob(analysisId, bamFile);
        basJob.setMaxMemory(memBasFileGet);
        basJob.addParent(gnosDownload); 
      } else {
        // then need to symlink so the downstream isn't broken
        gnosDownload = gnosSymlinkBaseJob(analysisId, bamFile);
        gnosDownload.setMaxMemory(memGnosDownload);
        gnosDownload.addParent(startTiming);
        String[] bamPath = bamFile.split("/");
        String newBamFile = analysisId + "/" + bamPath[bamPath.length - 1];
        // get the BAS files
        basJob = basFileBaseJob(analysisId, newBamFile);
        basJob.setMaxMemory(memBasFileGet);
        basJob.addParent(gnosDownload);   
      }
      
    }
    return basJob;
  }
  
  @Override
  public void buildWorkflow() {
    Job controlBasJob = null;
    String controlBam = null;
    List<String> tumourBams = new ArrayList<String>();
    List<Job> tumourBasJobs = new ArrayList<Job>();
    
    List<String> tumourAnalysisIds = new ArrayList<String>();
    List<String> tumourAliquotIds = new ArrayList<String>();
    String controlAnalysisId = new String();

    Job startDownload = markTime("download", "start");
    startDownload.setMaxMemory(memMarkTime);
    
    Job startWorkflow = markTime("workflow", "start");
    startWorkflow.setMaxMemory(memMarkTime);
    startWorkflow.addParent(startDownload);
    
    try {
      if(testMode) {
        controlBam = OUTDIR + "/HCC1143_BL.bam";
        controlAnalysisId = "HCC1143_BL";
        tumourBams.add(OUTDIR + "/HCC1143.bam");
        tumourAnalysisIds.add("HCC1143");
        tumourAliquotIds.add("HCC1143");
        
        controlBasJob = prepareTestData("HCC1143_BL");
        controlBasJob.setMaxMemory("4000");
        controlBasJob.setThreads(2);
        controlBasJob.addParent(startWorkflow);
        
        Job prepTum = prepareTestData("HCC1143");
        prepTum.setMaxMemory("4000");
        prepTum.setThreads(2);
        prepTum.addParent(startWorkflow);
        tumourBasJobs.add(prepTum);

        Job stopDownload = markTime("download", "end");
        stopDownload.setMaxMemory(memMarkTime);
        stopDownload.addParent(prepTum);

      } else {

        List<Job> downloadJobsList = new ArrayList<Job>();
        controlAnalysisId = getProperty("controlAnalysisId");
        if (localFileMode) {
          controlBam = getProperty("controlBam");
        } else {
          controlBam = controlAnalysisId + "/" + getProperty("controlBam");
        }
        controlBasJob = bamProvision(controlAnalysisId, controlBam, startWorkflow);
        // this is being done because the above makes a symlink to <analysisId>/<bamname.bam>
        // and subsequent steps expect this, so controlBam must be just the filename and not the full path
        if (localFileMode) {
          List<String> bamPath = Arrays.asList(getProperty("controlBam").split("/"));
          controlBam = controlAnalysisId + "/" + bamPath.get(bamPath.size() - 1);
        }
        downloadJobsList.add(controlBasJob);

        // TODO: just makes an echo command
        // optional upload of the downloaded tumor bam
        /*   private Job alignedBamUploadJob(Job parentJob, String bamDownloadServer, String studyRefnameOverride, String analysisCenter, String bamUploadServer, String bamUploadPemFile, String analysisId, String bamPath, String uploadScriptJobMem, String uploadScriptJobSlots) { */
        if (hasPropertyAndNotNull("bamUploadServer") && hasPropertyAndNotNull("bamUploadPemFile")) {
          Job normalBamUpload = alignedBamUploadJob(controlBasJob, gnosServer, bamUploadStudyRefnameOverride, bamUploadAnalysisCenterOverride, bamUploadServer, bamUploadPemFile, controlAnalysisId, getProperty("controlBam"), bamUploadScriptJobMem, bamUploadScriptJobSlots);
          normalBamUpload.addParent(controlBasJob);
        }

        tumourAnalysisIds = Arrays.asList(getProperty("tumourAnalysisIds").split(":"));
        tumourAliquotIds = Arrays.asList(getProperty("tumourAliquotIds").split(":"));
        List<String> rawBams = Arrays.asList(getProperty("tumourBams").split(":"));
        if(rawBams.size() != tumourAnalysisIds.size()) {
          throw new RuntimeException("Properties tumourAnalysisId and tumourBam decode to lists of different sizes");
        }
        if(rawBams.size() != tumourAliquotIds.size()) {
          throw new RuntimeException("Properties tumourAliquotIds and tumourBam decode to lists of different sizes");
        }
        for(int i=0; i<rawBams.size(); i++) {
          String tumourBam = rawBams.get(i);
          if (!localFileMode) {
            tumourBam = tumourAnalysisIds.get(i) + "/" + rawBams.get(i);
          }
          Job tumourBasJob = bamProvision(tumourAnalysisIds.get(i), tumourBam, startWorkflow);
          tumourBasJobs.add(tumourBasJob);
          if (localFileMode) {
            List<String> bamPath = Arrays.asList(tumourBam.split("/"));
            tumourBam = tumourAnalysisIds.get(i) + "/" + bamPath.get(bamPath.size() - 1);
          }
          tumourBams.add(tumourBam);
          downloadJobsList.add(tumourBasJob);

          // TODO: just makes an echo command
          // optional upload of the downloaded tumor bam
          if (hasPropertyAndNotNull("bamUploadServer") && hasPropertyAndNotNull("bamUploadPemFile")) {
            Job tumourBamUpload = alignedBamUploadJob(tumourBasJob, gnosServer, bamUploadStudyRefnameOverride, bamUploadAnalysisCenterOverride, bamUploadServer, bamUploadPemFile, tumourAnalysisIds.get(i), rawBams.get(i), bamUploadScriptJobMem, bamUploadScriptJobSlots);
            tumourBamUpload.addParent(tumourBasJob);
          }
        }

        // save timing info for downloads
        Job stopDownload = markTime("download", "end");
        stopDownload.setMaxMemory(memMarkTime);
        for (Job currJob : downloadJobsList) {
          stopDownload.addParent(currJob);
        }
      }

      Job getTbiJob = stageTbi();
      getTbiJob.setMaxMemory(memGetTbi);
      getTbiJob.addParent(startWorkflow);
      getTbiJob.addParent(controlBasJob);
      for(Job job : tumourBasJobs) {
        getTbiJob.addParent(job);
      }

      Job genotypeJob = genoptypeBaseJob(tumourBams, controlBam);
      genotypeJob.setMaxMemory(memGenotype);
      genotypeJob.addParent(getTbiJob);

      Job genotypePackJob = packageGenotype(tumourBams, controlBam);
      genotypePackJob.setMaxMemory("4000");
      genotypePackJob.addParent(genotypeJob);

      Job contaminationJob = contaminationBaseJob(tumourBams.size(), controlBam, "control");
      contaminationJob.setMaxMemory(memContam);
      contaminationJob.addParent(getTbiJob);
      // packaging must have parent cavemanTbiCleanJob

      // these are not paired but per individual sample
      List<Job> bbAlleleCountJobs = new ArrayList<Job>();
      for(int i=0; i<23; i++) { // not 1-22+X
        for(int j=0; j<tumourBams.size(); j++) {
          Job bbAlleleCountJob = bbAlleleCount(j, tumourBams.get(j), "tumour", i);
          bbAlleleCountJob.setMaxMemory(memAlleleCount);
          bbAlleleCountJob.addParent(getTbiJob);
          bbAlleleCountJobs.add(bbAlleleCountJob);
        }
        Job bbAlleleCountJob = bbAlleleCount(1, controlBam, "control", i);
        bbAlleleCountJob.setMaxMemory(memAlleleCount);
        bbAlleleCountJob.addParent(getTbiJob);
        bbAlleleCountJobs.add(bbAlleleCountJob);
      }

      Job bbAlleleMergeJob = bbAlleleMerge(controlBam);
      bbAlleleMergeJob.setMaxMemory(memBbMerge);
      for(Job j : bbAlleleCountJobs) {
        bbAlleleMergeJob.addParent(j);
      }

      // these are not paired but per individual sample
      List<Job> ngsCountJobs = new ArrayList<Job>();
      for(int i=1; i<=24; i++) {
        for(int j=0; j<tumourBams.size(); j++) {
          Job ngsCountJob = ngsCount(j, tumourBams.get(j), "tumour", i);
          ngsCountJob.setMaxMemory(memPicnicCounts);
          ngsCountJob.addParent(getTbiJob);
          ngsCountJobs.add(ngsCountJob);
        }
        Job ngsCountJob = ngsCount(1, controlBam, "control", i);
        ngsCountJob.setMaxMemory(memPicnicCounts);
        ngsCountJob.addParent(getTbiJob);
        ngsCountJobs.add(ngsCountJob);
      }

      Job ngsCountMergeJob = ngsCountMerge(controlBam);
      ngsCountMergeJob.setMaxMemory(memPicnicMerge);
      for(Job j : ngsCountJobs) {
        ngsCountMergeJob.addParent(j);
      }

      // donor based workflow section
      Job[] cavemanFlagJobs = new Job [tumourBams.size()];
      for(int i=0; i<tumourBams.size(); i++) {
        Job cavemanFlagJob = buildPairWorkflow(getTbiJob, controlBam, tumourBams.get(i), i);
        cavemanFlagJobs[i] = cavemanFlagJob;
      }

      Job cavemanTbiCleanJob = cavemanTbiCleanJob();
      cavemanTbiCleanJob.setMaxMemory(memCavemanTbiClean);
      cavemanTbiCleanJob.addParent(contaminationJob); // control contamination
      for(Job cavemanFlagJob : cavemanFlagJobs) {
        cavemanTbiCleanJob.addParent(cavemanFlagJob);
        // tumour contamination is linked in buildPairWorkflow()
      }

      Job endWorkflow = markTime("workflow", "end");
      endWorkflow.setMaxMemory(memMarkTime);
      endWorkflow.addParent(cavemanTbiCleanJob);

      Job metricsJob = getMetricsJob(tumourBams, controlBam);
      metricsJob.setMaxMemory(memQcMetrics);
      metricsJob.addParent(endWorkflow);

      Job renameGenotypeJob = renameSampleFile(tumourBams, OUTDIR, "genotype.tar.gz");
      renameGenotypeJob.setMaxMemory("4000");
      renameGenotypeJob.addParent(genotypePackJob);
      Job renameGenotypeMd5Job = renameSampleFile(tumourBams, OUTDIR, "genotype.tar.gz.md5");
      renameGenotypeMd5Job.setMaxMemory("4000");
      renameGenotypeMd5Job.addParent(genotypePackJob);

      Job packageContamJob = packageContam(tumourBams, controlBam);
      packageContamJob.setMaxMemory("4000");
      packageContamJob.addParent(cavemanTbiCleanJob);

      Job renameContamJob = renameSampleFile(tumourBams, OUTDIR, "verifyBamId.tar.gz");
      renameContamJob.setMaxMemory("4000");
      renameContamJob.addParent(packageContamJob);
      Job renameContamMd5Job = renameSampleFile(tumourBams, OUTDIR, "verifyBamId.tar.gz.md5");
      renameContamMd5Job.setMaxMemory("4000");
      renameContamMd5Job.addParent(packageContamJob);

      Job renameImputeJob = renameSampleFile(tumourBams, OUTDIR + "/bbCounts", "imputeCounts.tar.gz");
      renameImputeJob.setMaxMemory("4000");
      renameImputeJob.addParent(bbAlleleMergeJob);
      Job renameImputeMd5Job = renameSampleFile(tumourBams, OUTDIR + "/bbCounts", "imputeCounts.tar.gz.md5");
      renameImputeMd5Job.setMaxMemory("4000");
      renameImputeMd5Job.addParent(bbAlleleMergeJob);

      Job renameCountsJob = renameSampleFile(tumourBams, OUTDIR + "/ngsCounts", "binnedReadCounts.tar.gz");
      renameCountsJob.setMaxMemory("4000");
      renameCountsJob.addParent(ngsCountMergeJob);
      Job renameCountsMd5Job = renameSampleFile(tumourBams, OUTDIR + "/ngsCounts", "binnedReadCounts.tar.gz.md5");
      renameCountsMd5Job.setMaxMemory("4000");
      renameCountsMd5Job.addParent(ngsCountMergeJob);

      if(uploadServer != null || (hasPropertyAndNotNull("upload-skip") && Boolean.valueOf(getProperty("upload-skip")))) {

        // track all the upload jobs
        List<Job> uploadJobs = new ArrayList<Job>();
        
        String[] resultTypes = {"snv_mnv","cnv","sv","indel"};
        Job uploadJob = vcfUpload(resultTypes, controlAnalysisId, tumourAnalysisIds, tumourAliquotIds);
        uploadJob.setMaxMemory(memUpload);
        uploadJob.addParent(metricsJob);
        uploadJob.addParent(renameImputeJob);
        uploadJob.addParent(renameImputeMd5Job);
        uploadJob.addParent(renameCountsJob);
        uploadJob.addParent(renameCountsMd5Job);
        uploadJobs.add(uploadJob);

        // this is used to create a tarball of the upload directory and store it in the specified path
        if (hasPropertyAndNotNull("saveUploadArchive") && Boolean.valueOf(getProperty("saveUploadArchive")) && hasPropertyAndNotNull("uploadArchivePath")) {

          if (hasPropertyAndNotNull("SFTPUploadArchive") && Boolean.valueOf(getProperty("SFTPUploadArchive"))) {
            // this is used to copy the contents of the upload directory to an SFTP server
            Job uploadToSFTP = uploadArchiveToSFTP(uploadArchivePath + "/" + uuid + ".tar.gz");
            uploadToSFTP.setMaxMemory(duckJobMem);
            uploadToSFTP.addParent(uploadJob);
            uploadJobs.add(uploadToSFTP);
          }
          if (hasPropertyAndNotNull("S3UploadArchive") && Boolean.valueOf(getProperty("S3UploadArchive"))) {
            // this is used to copy the contents of the upload directory to an SFTP server
            Job uploadToS3 = uploadArchiveToS3(uploadArchivePath + "/" + uuid + ".tar.gz");
            uploadToS3.setMaxMemory(duckJobMem);
            uploadToS3.addParent(uploadJob);
            uploadJobs.add(uploadToS3);
          }
        }
        
        if (hasPropertyAndNotNull("SFTPUploadFiles") && Boolean.valueOf(getProperty("SFTPUploadFiles"))) {
          // this is used to copy the contents of the upload directory to an SFTP server
          Job uploadToSFTP = uploadFilesToSFTP(uploadArchivePath, uuid, tumourAliquotIds);
          uploadToSFTP.setMaxMemory(duckJobMem);
          uploadToSFTP.addParent(uploadJob);
          uploadJobs.add(uploadToSFTP);
        }
        if (hasPropertyAndNotNull("S3UploadFiles") && Boolean.valueOf(getProperty("S3UploadFiles"))) {
          // this is used to copy the contents of the upload directory to an SFTP server
          Job uploadToS3 = uploadFilesToS3(uploadArchivePath, uuid, tumourAliquotIds);
          uploadToS3.setMaxMemory(duckJobMem);
          uploadToS3.addParent(uploadJob);
          uploadJobs.add(uploadToS3);
        }
        if (SynapseUpload) {
          // this is used to copy the contents of the upload directory to an SFTP server
          Job uploadToSynapse = uploadFilesToSynapse(uploadArchivePath, uuid);
          uploadToSynapse.setMaxMemory(duckJobMem);
          uploadToSynapse.addParent(uploadJob);
          uploadJobs.add(uploadToSynapse);
        }

        if (cleanup || cleanupBams) {
          // if we upload to GNOS then go ahead and delete all the large files
          Job cleanJob = cleanJob();
          for(Job currUpload : uploadJobs) {
            cleanJob.addParent(currUpload);
          }
        }

      } else {
        // delete just the BAM inputs and not the output dir
        if (cleanup || cleanupBams) {
          Job cleanInputsJob = cleanJob();
          cleanInputsJob.addParent(metricsJob);
        }
      }
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
  
    private Job uploadFilesToSynapse(String uploadPath, String uuid) {
      Job upload = getWorkflow().createBashJob("uploadFilesToSynapse");
      upload.getCommand()
          .addArgument("cp "+uploadPath+"/"+uuid+"/analysis.xml "+uploadPath+"/"+uuid+"/"+uuid+".analysis.xml;")
          .addArgument("synapse login -u '"+SynapseUploadUsername+"' -p '"+SynapseUploadPassword+"'  --rememberMe ;")
          .addArgument("echo '[sftp://tcgaftps.nci.nih.gov]\n" +
                       "username = "+SynapseUploadSFTPUsername+"\n" +
                       "password = "+SynapseUploadSFTPPassword+"' > ~/.synapseConfig; ")
          .addArgument("perl " + getWorkflowBaseDir() + "/bin/synapse_upload_vcf.pl --local-xml "+uploadPath+"/"+uuid+"/"+uuid+".analysis.xml --local-path "+uploadPath+"/"+uuid+" --parent-id "+SynapseUploadParent+" --sftp-url "+SynapseUploadURL+" ; ");
      return(upload);
    }

    private Job uploadFilesToSFTP(String uploadPath, String uuid, List<String> tumourAliquotIds) {
      Job upload = getWorkflow().createBashJob("uploadFilesToSFTP");
      String glob = "";
      for(String tumourAliquotId : tumourAliquotIds) {
        glob = glob + " `pwd`/" + tumourAliquotId + ".*";
      }
      upload.getCommand()
          .addArgument("cp "+uploadPath+"/"+uuid+"/analysis.xml "+uploadPath+"/"+uuid+"/"+uuid+".analysis.xml;")
          .addArgument("cd "+uploadPath+"/"+uuid+";")
          .addArgument("duck -e " + SFTPUploadMode + " -y -r -p '" + SFTPUploadPassword + "' -u " + SFTPUploadUsername + " --upload  sftp://" + SFTPUploadServer + "/" + SFTPUploadPath + " " + glob + " `pwd`/" +uuid+".analysis.xml;");
      return(upload);
    }

    private Job uploadFilesToS3(String uploadPath, String uuid, List<String> tumourAliquotIds) {
      Job upload = getWorkflow().createBashJob("uploadFilesToS3");
      String glob = "";
      for(String tumourAliquotId : tumourAliquotIds) {
        glob = glob + " `pwd`/" + tumourAliquotId + ".*";
      }
      upload.getCommand()
          .addArgument("cp "+uploadPath+"/"+uuid+"/analysis.xml "+uploadPath+"/"+uuid+"/"+uuid+".analysis.xml;")
          .addArgument("cd "+uploadPath+"/"+uuid+";")
          .addArgument("duck -e " + S3UploadFileMode + " -y -r -p '" + S3UploadSecretKey + "' -u " + S3UploadKey + " --upload  " + S3UploadBucketURL + " " + glob + " `pwd`/" +uuid+".analysis.xml;");
      return(upload);
    }

    private Job uploadArchiveToSFTP(String archivePath) {
      Job upload = getWorkflow().createBashJob("uploadArchiveToSFTP");
      //  sshpass -p 'password' sftp -o StrictHostKeyChecking=no username@tcgaftps.nci.nih.gov:/tcgapancan/pancan/variant_calling_pilot_64/OICR_Sanger_Core
      // duck -e overwrite -r -p 'password' -u username -d sftp://tcgaftps.nci.nih.gov/tcgapancan/pancan/variant_calling_pilot_64/OICR_Sanger_Core/f9c3bc8e-dbc4-1ed0-e040-11ac0d4803a9.svcp_1-0-2.20150106.somatic.sv.vcf.gz.tbi test.tbi
      upload.getCommand().addArgument("duck -e " + SFTPUploadArchiveMode + " -y -r -p '" + SFTPUploadArchivePassword + "' -u " + SFTPUploadArchiveUsername + " --upload  sftp://" + SFTPUploadArchiveServer + "/" + SFTPUploadArchivePath + " `readlink -f " +  archivePath + "`");
      return(upload);
    }

    private Job uploadArchiveToS3(String archivePath) {
      Job upload = getWorkflow().createBashJob("uploadArchiveToS3");
      // duck -e skip -r -p 'secretkey' -u 'key' -d s3://pan-cancer-testing/m2.tar.gz m2.tar.gz
      upload.getCommand().addArgument("duck -e " + S3UploadArchiveMode + " -y -r -p '" + S3UploadArchiveSecretKey + "' -u '" + S3UploadArchiveKey + "' --upload  " + S3UploadArchiveBucketURL + " `readlink -f " +  archivePath + "`");
      return(upload);
    }

    /**
   * This builds the workflow for a pair of samples
   * The generic buildWorkflow section will choose the pair to be processed and 
   * setup the control sample download
   */
  private Job buildPairWorkflow(Job getTbiJob, String controlBam, String tumourBam, int tumourCount) {
    
    /**
     * ASCAT - Copynumber
     * Depends on
     *  - tumour/control BAMs
     *  - Gender, will attempt to determine if not specified
     */

    Job[] alleleCountJobs = new Job[2];
    for(int i=0; i<2; i++) {
      Job alleleCountJob = cgpAscatBaseJob(tumourCount, tumourBam, controlBam, "ASCAT", "allele_count", i+1);
      alleleCountJob.setMaxMemory(memAlleleCount);
      alleleCountJob.addParent(getTbiJob);
      alleleCountJobs[i] = alleleCountJob;
    }

    Job ascatJob = cgpAscatBaseJob(tumourCount, tumourBam, controlBam, "ASCAT", "ascat", 1);
    ascatJob.setMaxMemory(memAscat);
    ascatJob.addParent(alleleCountJobs[0]);
    ascatJob.addParent(alleleCountJobs[1]);
    
    Job ascatFinaliseJob = cgpAscatBaseJob(tumourCount, tumourBam, controlBam, "ASCAT", "finalise", 1);
    ascatFinaliseJob.setMaxMemory(memAscatFinalise);
    ascatFinaliseJob.addParent(ascatJob);
    
    Job ascatPackage = packageResults(tumourCount, "ascat", "cnv", tumourBam, "copynumber.caveman.vcf.gz", workflowName, "somatic", dateString);
    ascatPackage.setMaxMemory(memPackageResults);
    ascatPackage.addParent(ascatFinaliseJob);
    
    Job contaminationJob = contaminationBaseJob(tumourCount, tumourBam, "tumour");
    contaminationJob.setMaxMemory(memContam);
    contaminationJob.addParent(ascatFinaliseJob);
    
    /**
     * CaVEMan setup is here to allow better workflow graph
     * Messy but necessary
     */
    
    Job caveCnPrepJobs[] = new Job[2];
    for(int i=0; i<2; i++) {
      Job caveCnPrepJob;
      if(i==0) {
        caveCnPrepJob = caveCnPrep(tumourCount, "tumour");
      }
      else {
        caveCnPrepJob = caveCnPrep(tumourCount, "normal");
      }
      caveCnPrepJob.addParent(getTbiJob);
      caveCnPrepJob.addParent(ascatFinaliseJob); // ASCAT dependency!!!
      caveCnPrepJobs[i] = caveCnPrepJob;
    }
    
    
    Job cavemanSetupJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "setup", 1);
    cavemanSetupJob.setMaxMemory(memCavemanSetup);
    cavemanSetupJob.addParent(caveCnPrepJobs[0]);
    cavemanSetupJob.addParent(caveCnPrepJobs[1]);
    // some dependencies handled by ascat step
  
    /**
     * Pindel - InDel calling
     * Depends on:
     *  - tumour/control BAMs
     */
     
    Job[] pindelInputJobs = new Job[2];
    for(int i=0; i<2; i++) {
      Job inputParse = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "input", i+1);
      inputParse.setMaxMemory(memPindelInput);
      inputParse.addParent(getTbiJob);
      pindelInputJobs[i] = inputParse;
    }
    
    int pindelNormalisedThreads = pindelNormalisedThreads(memPindelPerThread, coresAddressable);
    int totalPindelMem = Integer.valueOf(memPindelPerThread) + (Integer.valueOf(memWorkflowOverhead) / pindelNormalisedThreads);
    
    Job pindelJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "pindel", 1);
    pindelJob.getCommand().addArgument("-l " + pindelNormalisedThreads);
    pindelJob.getCommand().addArgument("-c " + pindelNormalisedThreads);
    pindelJob.setMaxMemory(Integer.toString(totalPindelMem));
    pindelJob.setThreads(pindelNormalisedThreads);
    pindelJob.addParent(pindelInputJobs[0]);
    pindelJob.addParent(pindelInputJobs[1]);
    
    // determine number of refs to process
    // we know that this is static for PanCancer so be lazy 24 jobs (1-22,X,Y)
    // but pindel needs to know the exclude list so hard code this
    Job pinVcfJobs[] = new Job[24];
    for(int i=0; i<24; i++) {
      Job pinVcfJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "pin2vcf", i+1);
      pinVcfJob.setMaxMemory(memPindelVcf);
      pinVcfJob.addParent(pindelJob);
      pinVcfJobs[i] = pinVcfJob;
    }
    
    Job pindelMergeJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "merge", 1);
    pindelMergeJob.setMaxMemory(memPindelMerge);
    for (Job parent : pinVcfJobs) {
      pindelMergeJob.addParent(parent);
    }
    
    Job pindelFlagJob = pindelBaseJob(tumourCount, tumourBam, controlBam, "cgpPindel", "flag", 1);
    pindelFlagJob.setMaxMemory(memPindelFlag);
    pindelFlagJob.addParent(pindelMergeJob);
    pindelFlagJob.addParent(cavemanSetupJob);
    
    Job pindelPackage = packageResults(tumourCount, "pindel", "indel", tumourBam, "flagged.vcf.gz", workflowName, "somatic", dateString);
    pindelPackage.setMaxMemory(memPackageResults);
    pindelPackage.addParent(pindelFlagJob);
    
    /**
     * BRASS - BReakpoint AnalySiS
     * Depends on:
     *  - tumour/control BAMs
     *  - ASCAT output at filter step
     */
    
    Job brassInputJobs[] = new Job[2];
    for(int i=0; i<2; i++) {
      Job brassInputJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "input", i+1);
      brassInputJob.setMaxMemory(memBrassInput);
      brassInputJob.addParent(getTbiJob);
      brassInputJobs[i] = brassInputJob;
    }
    
    Job brassGroupJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "group", 1);
    brassGroupJob.setMaxMemory(memBrassGroup);
    brassGroupJob.addParent(brassInputJobs[0]);
    brassGroupJob.addParent(brassInputJobs[1]);
    
    Job brassFilterJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "filter", 1);
    brassFilterJob.setMaxMemory(memBrassFilter);
    brassFilterJob.addParent(brassGroupJob);
    brassFilterJob.addParent(ascatFinaliseJob); // NOTE: dependency on ASCAT!!
    
    Job brassSplitJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "split", 1);
    brassSplitJob.setMaxMemory(memBrassSplit);
    brassSplitJob.addParent(brassFilterJob);

    
    int brassAssNormalisedThreads = getMemNormalisedThread(memBrassAssemblePerThread, coresAddressable);
    int totalBrassAssMem = Integer.valueOf(memBrassAssemblePerThread) + (Integer.valueOf(memWorkflowOverhead) / brassAssNormalisedThreads);
    
    Job brassAssembleJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "assemble", 1);
    brassAssembleJob.setMaxMemory(Integer.toString(totalBrassAssMem));
    brassAssembleJob.getCommand().addArgument("-l " + brassAssNormalisedThreads);
    brassAssembleJob.getCommand().addArgument("-c " + brassAssNormalisedThreads);
    brassAssembleJob.setMaxMemory(Integer.toString(totalBrassAssMem));
    brassAssembleJob.setThreads(brassAssNormalisedThreads);
    brassAssembleJob.addParent(brassSplitJob);
    
    Job brassGrassJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "grass", 1);
    brassGrassJob.setMaxMemory(memBrassGrass);
    brassGrassJob.addParent(brassAssembleJob);
    
    Job brassTabixJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "tabix", 1);
    brassTabixJob.setMaxMemory(memBrassTabix);
    brassTabixJob.addParent(brassGrassJob);
    
    Job brassPackage = packageResults(tumourCount, "brass", "sv", tumourBam, "annot.vcf.gz", workflowName, "somatic", dateString);
    brassPackage.setMaxMemory(memPackageResults);
    brassPackage.addParent(brassTabixJob);
    
    
    /**
     * CaVEMan - SNV analysis
     * !! see above as setup done earlier to help with workflow structure !!
     * Depends on:
     *  - tumour/control BAMs (but depend on BAS for better workflow)
     *  - ASCAT from outset
     *  - pindel at flag step
     */
    
    // should really line count the fai file
    Job cavemanSplitJobs[] = new Job[86];
    for(int i=0; i<86; i++) {
      Job cavemanSplitJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "split", i+1);
      cavemanSplitJob.setMaxMemory(memCavemanSplit);
      cavemanSplitJob.addParent(cavemanSetupJob);
      cavemanSplitJobs[i] = cavemanSplitJob;
    }
    
    Job cavemanSplitConcatJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "split_concat", 1);
    cavemanSplitConcatJob.setMaxMemory(memCavemanSplitConcat);
    for (Job cavemanSplitJob : cavemanSplitJobs) {
      cavemanSplitConcatJob.addParent(cavemanSplitJob);
    }
    
    int mstepNormalisedThreads = getMemNormalisedThread(memCavemanMstepPerThread, coresAddressable);
    int totalMstepMem = Integer.valueOf(memCavemanMstepPerThread) + (Integer.valueOf(memWorkflowOverhead) / mstepNormalisedThreads);
    
    Job cavemanMstepJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "mstep", 1);
    cavemanMstepJob.getCommand().addArgument("-l " + mstepNormalisedThreads);
    cavemanMstepJob.getCommand().addArgument("-t " + mstepNormalisedThreads);
    cavemanMstepJob.setMaxMemory(Integer.toString(totalMstepMem));
    cavemanMstepJob.setThreads(mstepNormalisedThreads);
    cavemanMstepJob.addParent(cavemanSplitConcatJob);
    
    Job cavemanMergeJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "merge", 1);
    cavemanMergeJob.setMaxMemory(memCavemanMerge);
    cavemanMergeJob.addParent(cavemanMstepJob);
    
    int estepNormalisedThreads = getMemNormalisedThread(memCavemanEstepPerThread, coresAddressable);
    int totalEstepMem = Integer.valueOf(memCavemanEstepPerThread) + (Integer.valueOf(memWorkflowOverhead) / estepNormalisedThreads);
    
    Job cavemanEstepJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "estep", 1);
    cavemanEstepJob.getCommand().addArgument("-l " + estepNormalisedThreads);
    cavemanEstepJob.getCommand().addArgument("-t " + estepNormalisedThreads);
    cavemanEstepJob.setMaxMemory(Integer.toString(totalEstepMem));
    cavemanEstepJob.setThreads(estepNormalisedThreads);
    cavemanEstepJob.addParent(cavemanMergeJob);
    
    Job cavemanMergeResultsJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "merge_results", 1);
    cavemanMergeResultsJob.setMaxMemory(memCavemanMergeResults);
    cavemanMergeResultsJob.addParent(cavemanEstepJob);
    
    Job cavemanAddIdsJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "add_ids", 1);
    cavemanAddIdsJob.setMaxMemory(memCavemanAddIds);
    cavemanAddIdsJob.addParent(cavemanMergeResultsJob);
    
    Job cavemanFlagJob = cavemanBaseJob(tumourCount, tumourBam, controlBam, "CaVEMan", "flag", 1);
    cavemanFlagJob.setMaxMemory(memCavemanFlag);
    cavemanFlagJob.addParent(getTbiJob);
    cavemanFlagJob.addParent(pindelFlagJob); // PINDEL dependency
    cavemanFlagJob.addParent(cavemanAddIdsJob);
    cavemanFlagJob.addParent(contaminationJob);
    
    Job cavemanPackage = packageResults(tumourCount, "caveman", "snv_mnv", tumourBam, "flagged.muts.vcf.gz", workflowName, "somatic", dateString);
    cavemanPackage.setMaxMemory(memPackageResults);
    cavemanPackage.addParent(cavemanFlagJob);
    
    return cavemanFlagJob;
  }
  
  /**
   * This allows for other processes to run along side if the number of cores
   * will allow.  Added in an attempt to prevent increased wall time for the
   * whole workflow.
   * 
   * @param memPerThread Memory in MB consumed by each thread
   * @param totalCores Total number of cores available to the workflow
   * @return Number of threads usable based on per-thread and system overhead.
   */
  private int pindelNormalisedThreads(String memPerThread, int totalCores) {
    int sensibleCores = totalCores;
    if(sensibleCores > 12) {
      sensibleCores = totalCores - 2;
    }
    int pindelNormalisedThreads = getMemNormalisedThread(memPerThread, sensibleCores);
    return pindelNormalisedThreads;
  }
  
  private Job prepareTestData(String sample) {
    Job thisJob = getWorkflow().createBashJob("prepTest");
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument("scramble -I cram -O bam")
      .addArgument("-r " + testBase + "/genome.fa")
      .addArgument("-t 2")
      .addArgument(testBase + "/" + sample + ".cram")
      .addArgument(OUTDIR + "/" + sample + ".bam")
      .addArgument("; cp " + testBase + "/" + sample + ".bam.bas")
      .addArgument(OUTDIR + "/.")
      .addArgument("; samtools index " + OUTDIR + "/" + sample + ".bam")
      ;
    return thisJob;
  }
  
  private Job ngsCountMerge(String controlBam) {
    Job thisJob = prepTimedJob(0, "binCount", "merge", 1);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("ngs_bin_allele_merge.pl")
              .addArgument(controlBam)
              .addArgument(COUNTDIR)
              ;
    return thisJob;
  }
  
  private Job ngsCount(int sampleIndex, String bam, String process, int index) {
    String chr = Integer.toString(index+1);
    if(index+1 == 23) {
      chr = "X";
    }
    else if(index+1 == 24) {
      chr = "Y";
    }
    
    Job thisJob = prepTimedJob(sampleIndex, "binCounts", process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("ngs_bin_allele.pl")
              .addArgument(refBase + "/picnic/cn_bins.csv.gz")
              .addArgument(refBase + "/picnic/snp6.csv.gz")
              .addArgument(COUNTDIR)
              .addArgument(bam)
              .addArgument(chr)
              ;
    return thisJob;
  }
  
  private Job bbAlleleCount(int sampleIndex, String bam, String process, int index) {
    Job thisJob = prepTimedJob(sampleIndex, "bbAllele", process, index);
    int chr = index+1;
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument(getWorkflowBaseDir()+ "/bin/execute_with_sample.pl " + bam)
              .addArgument("alleleCounter")
              .addArgument("-l " + refBase + "/battenberg/1000genomesloci/1000genomesloci2012_chr" + chr + ".txt")
              .addArgument("-o " + BBDIR + "/%SM%." + chr + ".tsv")
              .addArgument("-b " + bam)
              ;
    return thisJob;
  }
  
  private Job bbAlleleMerge(String controlBam) {
    Job thisJob = prepTimedJob(0, "bbAllele", "merge", 1);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument(getWorkflowBaseDir()+ "/bin/packageImpute.pl")
              .addArgument(controlBam)
              .addArgument(BBDIR)
              ;
    return thisJob;
  }
  
  private Job renameSampleFile(List<String> bams, String dir, String extension) {
    Job thisJob = getWorkflow().createBashJob("renameSampleFile");
    for(String bam : bams) {
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument(getWorkflowBaseDir()+ "/bin/execute_with_sample.pl " + bam)
      .addArgument("cp " + dir + "/" + "%SM%." + extension)
      .addArgument(OUTDIR + "/" + "%SM%." + workflowName + "." + dateString + ".somatic." + extension)
      .addArgument(";");
      ;
    }
    return thisJob;
  }
  
  private Job vcfUpload(String[] types, String controlAnalysisId, List<String> tumourAnalysisIds, List<String> tumourAliquotIds) {
    Job thisJob = getWorkflow().createBashJob("vcfUpload");
    
    String metadataUrls = new String();
    metadataUrls = metadataUrls.concat(gnosServer)
                              .concat("/cghub/metadata/analysisFull/")
                              .concat(controlAnalysisId);
    for(String tumourAnalysisId : tumourAnalysisIds) {
      metadataUrls = metadataUrls.concat(",")
                              .concat(gnosServer)
                              .concat("/cghub/metadata/analysisFull/")
                              .concat(tumourAnalysisId);
                              
    }
    
    String vcfs = new String();
    String tbis = new String();
    String tars = new String();
    String vcfmd5s = new String();
    String tbimd5s = new String();
    String tarmd5s = new String();
    for(String type: types) {
      for(String tumourAliquotId : tumourAliquotIds) {
        // TODO: Hardcoded somatic here, is that correct?
        String baseFile = OUTDIR + "/" + tumourAliquotId + "." + workflowName + "." + dateString + ".somatic." + type;
        if(vcfs.length() > 0) {
          vcfs = vcfs.concat(",");
          tbis = tbis.concat(",");
          tars = tars.concat(",");
          vcfmd5s = vcfmd5s.concat(",");
          tbimd5s = tbimd5s.concat(",");
          tarmd5s = tarmd5s.concat(",");
        }
        vcfs = vcfs.concat(baseFile + ".vcf.gz");
        tbis = tbis.concat(baseFile + ".vcf.gz.tbi");
        tars = tars.concat(baseFile + ".tar.gz");
        vcfmd5s = vcfmd5s.concat(baseFile + ".vcf.gz.md5");
        tbimd5s = tbimd5s.concat(baseFile + ".vcf.gz.tbi.md5");
        tarmd5s = tarmd5s.concat(baseFile + ".tar.gz.md5");
      }
    }
    
    // specific to CGP data
    for(String tumourAliquotId : tumourAliquotIds) {
      String baseFile = OUTDIR + "/" + tumourAliquotId + "." + workflowName + "." + dateString + ".somatic.";
      tars = tars.concat("," + baseFile + "imputeCounts.tar.gz");
      tars = tars.concat("," + baseFile + "binnedReadCounts.tar.gz");
      tars = tars.concat("," + baseFile + "genotype.tar.gz");
      tars = tars.concat("," + baseFile + "verifyBamId.tar.gz");
      tarmd5s = tarmd5s.concat("," + baseFile + "imputeCounts.tar.gz.md5");
      tarmd5s = tarmd5s.concat("," + baseFile + "binnedReadCounts.tar.gz.md5");
      tarmd5s = tarmd5s.concat("," + baseFile + "genotype.tar.gz.md5");
      tarmd5s = tarmd5s.concat("," + baseFile + "verifyBamId.tar.gz.md5");
    }

    thisJob.getCommand()
      .addArgument("perl -I " + getWorkflowBaseDir() + "/bin/lib " + getWorkflowBaseDir() + "/bin/gnos_upload_vcf.pl")
      .addArgument("--metadata-urls " + metadataUrls)
      .addArgument("--vcfs " + vcfs)
      .addArgument("--vcf-md5sum-files " + vcfmd5s)
      .addArgument("--vcf-idxs " + tbis)
      .addArgument("--vcf-idx-md5sum-files " + tbimd5s)
      .addArgument("--tarballs " + tars)
      .addArgument("--tarball-md5sum-files " + tarmd5s)
      .addArgument("--outdir " + OUTDIR + "/upload")
      .addArgument("--key " + uploadPemFile)
      .addArgument("--upload-url " + uploadServer)
      .addArgument("--qc-metrics-json " + OUTDIR + "/qc_metrics.json")
      .addArgument("--timing-metrics-json " + OUTDIR + "/process_metrics.json")
      .addArgument("--workflow-src-url "+Version.WORKFLOW_SRC_URL)
      .addArgument("--workflow-url "+Version.WORKFLOW_URL)
      .addArgument("--workflow-name " + Version.WORKFLOW_NAME)
      .addArgument("--workflow-version " + Version.WORKFLOW_VERSION)
      .addArgument("--seqware-version " + Version.SEQWARE_VERSION)
      .addArgument("--vm-instance-type " + vmInstanceType)
      .addArgument("--vm-instance-cores " +vmInstanceCores)
      .addArgument("--vm-instance-mem-gb " +vmInstanceMemGb)
      .addArgument("--vm-location-code " +vmLocationCode)
      .addArgument("--uuid " + uuid)
      ;
    try {
      if (hasPropertyAndNotNull("saveUploadArchive") && hasPropertyAndNotNull("uploadArchivePath") && "true".equals(getProperty("saveUploadArchive"))) {
        thisJob.getCommand().addArgument("--upload-archive "+ getProperty("uploadArchivePath"));
      }
      if(hasPropertyAndNotNull("study-refname-override")) {
        thisJob.getCommand().addArgument("--study-refname-override " + getProperty("study-refname-override"));
      }
      if(hasPropertyAndNotNull("analysis-center-override")) {
        thisJob.getCommand().addArgument("--analysis-center-override " + getProperty("analysis-center-override"));
      }
      if(hasPropertyAndNotNull("center-override")) {
        thisJob.getCommand().addArgument("--center-override " + getProperty("center-override"));
      }
      if(hasPropertyAndNotNull("ref-center-override")) {
        thisJob.getCommand().addArgument("--ref-center-override " + getProperty("ref-center-override"));
      }
      if(hasPropertyAndNotNull("upload-test") && Boolean.valueOf(getProperty("upload-test"))) {
        thisJob.getCommand().addArgument("--test ");
      }
      if(hasPropertyAndNotNull("upload-skip") && Boolean.valueOf(getProperty("upload-skip"))) {
        thisJob.getCommand().addArgument("--skip-upload");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    return thisJob;
  }
  
  private Job packageResults(int tumourCount, String algName, String resultType, String tumourBam, String baseVcf, String workflowName, String somaticOrGermline, String date) {
    //#packageResults.pl outdir 0772aed3-4df7-403f-802a-808df2935cd1/c007f362d965b32174ec030825262714.bam outdir/caveman snv_mnv flagged.muts.vcf.gz
    Job thisJob = getWorkflow().createBashJob("packageResults");
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument(getWorkflowBaseDir() + "/bin/packageResults.pl")
              .addArgument(OUTDIR)
              .addArgument(tumourBam)
              .addArgument(OUTDIR + "/" + tumourCount + "/" + algName)
              .addArgument(resultType)
              .addArgument(baseVcf)
              .addArgument(workflowName)
              .addArgument(somaticOrGermline)
              .addArgument(date)
      ;
    return thisJob;
  }
  
  private int getMemNormalisedThread(String perThreadMemory, int threads) {
    int usableThreads = 0;
    // memWorkflowOverhead, memHostMbAvailable
    
    int memoryAvail = memHostMbAvailable - memWorkflowOverhead;
    
    if((memoryAvail / threads) > Integer.valueOf(perThreadMemory)) {
      usableThreads = threads;
    }
    else {
      usableThreads = memoryAvail / Integer.valueOf(perThreadMemory);
    }
    
    return usableThreads;
  }
  
  private Job gnosDownloadBaseJob(String analysisId, String bamFile) {
    Job thisJob = getWorkflow().createBashJob("GNOSDownload");
    /*
    perl -I ../gt-download-upload-wrapper/lib gnos_download_file.pl --command 'gtdownload -c /mnt/home/seqware/.ssh/boconnor_gnos_ebi_keyfile.pem -v https://gtrepo-ebi.annailabs.com/cghub/data/analysis/download/96e252b8-911a-44c7-abc6-b924845e0be6' --file 96e252b8-911a-44c7-abc6b924845e0be6/7d743b10ea1231730151b2c9d91c527f.bam --retries 10 --sleep-min 1 --timeout-min 60
    */
    thisJob.getCommand()
                  .addArgument("perl -I " + getWorkflowBaseDir() + "/bin/lib " + getWorkflowBaseDir() + "/bin/gnos_download_file.pl ")
                  .addArgument("--command 'gtdownload -c " + pemFile )
                  .addArgument(" -l ./download"+analysisId+".log")
                  .addArgument(" -k 60")
                  .addArgument("-vv " + gnosServer + "/cghub/data/analysis/download/" + analysisId + "'")
                  .addArgument("--file " + bamFile)
                  .addArgument("--retries 10 --sleep-min 1 --timeout-min 60");
                  /*.addArgument("gtdownload -c " + pemFile)
                  .addArgument("-v " + gnosServer + "/cghub/data/analysis/download/" + analysisId); */
    return thisJob;
  }

  private Job gnosSymlinkBaseJob(String analysisId, String bamFile) {
    Job thisJob = getWorkflow().createBashJob("GNOSSymlink");

    thisJob.getCommand()
        .addArgument("mkdir -p " + analysisId + "; ln -s "+bamFile+" "+analysisId+"/");

    return thisJob;
  }
  
  private Job basFileBaseJob(String analysisId, String sampleBam) {
    Job thisJob = getWorkflow().createBashJob("basFileGet");
    thisJob.getCommand()
            .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
            .addArgument(installBase)
            .addArgument("xml_to_bas.pl")
            .addArgument("-d " + gnosServer + "/cghub/metadata/analysisFull/" + analysisId)
            .addArgument("-b " + sampleBam)
            .addArgument("-o " + sampleBam + ".bas")
            ;
    return thisJob;
  }
  
  private Job getMetricsJob(List<String> tumourBams, String controlBam) {
    //die "USAGE: rootOfOutdir ordered.bam [ordered.bam2]";
    Job thisJob = getWorkflow().createBashJob("metrics");
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument(getWorkflowBaseDir()+ "/bin/qc_and_metrics.pl")
      .addArgument(OUTDIR)
      .addArgument(controlBam);
    for(String bam : tumourBams) {
      thisJob.getCommand().addArgument(bam);
    }
    return thisJob;
  }
  
  private Job caveCnPrep(int tumourCount, String type) {
    String cnPath = OUTDIR + "/" + tumourCount + "/ascat/*.copynumber.caveman.csv";
    
    Job thisJob = getWorkflow().createBashJob("CaveCnPrep" + type);
    int offset = 0;
    if(type.equals("tumour")) {
      offset = 6;
    }
    else if(type.equals("normal")) {
      offset = 4;
    }
    thisJob.getCommand()
      .addArgument("perl -ne '@F=(split q{,}, $_)[1,2,3," + offset + "]; $F[1]-1; print join(\"\\t\",@F).\"\\n\";'")
      .addArgument("< " + cnPath)
      .addArgument("> " + OUTDIR + "/" + tumourCount + "/" + type + ".cn.bed")
      ;
    thisJob.setMaxMemory(memCaveCnPrep);
    return thisJob;
  }
  
  private Job cavemanTbiCleanJob() {
    Job thisJob = getWorkflow().createBashJob("CaveTbiClean");
      thisJob.getCommand().addArgument("rm -f " + OUTDIR + "/unmatchedNormal.*.vcf.gz.tbi");
    return thisJob;
  }
  
  private Job cleanJob() {
    Job thisJob = getWorkflow().createBashJob("GeneralCleanup");
    // this just removes the contents of the working directory and not OUTDIR which may point to another filesystem for archival purposes
    if (cleanupBams) {
      thisJob.getCommand().addArgument("rm -f ./*/*.bam; ");
    }
    // this removes the whole working directory
    if (cleanup) {
      thisJob.getCommand().addArgument("rm -rf .; ");
    }
    return thisJob;
  }
  
  private Job cavemanBaseJob(int tumourCount, String tumourBam, String controlBam, String alg, String process, int index) {
    String ascatContamFile = OUTDIR + "/" + tumourCount + "/ascat/*.samplestatistics.csv";
    
    Job thisJob = prepTimedJob(tumourCount, alg, process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("caveman.pl")
              .addArgument("-p " + process)
              .addArgument("-ig " + refBase + "/caveman/ucscHiDepth_0.01_merge1000_no_exon.tsv")
              .addArgument("-b " + refBase + "/caveman/flagging")
              .addArgument("-u " + tabixSrvUri)
              .addArgument("-np " + seqType)
              .addArgument("-tp " + seqType)
              .addArgument("-sa " + assembly)
              .addArgument("-s " + species)
              .addArgument("-st " + seqProtocol)
              .addArgument("-o " + OUTDIR + "/" + tumourCount + "/caveman")
              .addArgument("-tc " + OUTDIR + "/" + tumourCount + "/tumour.cn.bed")
              .addArgument("-nc " + OUTDIR + "/" + tumourCount + "/normal.cn.bed")
              .addArgument("-k " + ascatContamFile)
              .addArgument("-tb " + tumourBam)
              .addArgument("-nb " + controlBam)
              .addArgument("-r " + genomeFaGz + ".fai");
    
    if(!process.equals("mstep") && !process.equals("estep")) {
      thisJob.getCommand().addArgument("-i " + index);
    }
    
    if(process.equals("flag")) {
      thisJob.getCommand().addArgument("-in " + OUTDIR + "/" + tumourCount + "/pindel/*.germline.bed");
    }

    return thisJob;
  }
  
  private Job genoptypeBaseJob(List<String> tumourBams, String controlBam) {
    Job thisJob = prepTimedJob(0, "compareBamGenotypes", "all", 0);
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument("compareBamGenotypes.pl")
      .addArgument("-o " + OUTDIR + "/genotype")
      .addArgument("-nb " + controlBam)
      .addArgument("-j " + OUTDIR + "/genotype/summary.json")
      ;
    for(String tumourBam : tumourBams) {
      thisJob.getCommand().addArgument("-tb " + tumourBam);
    }
    return thisJob;
  }
  
  private Job packageGenotype(List<String> tumourBams, String controlBam) {
    Job thisJob = getWorkflow().createBashJob("packageGenotype");
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument(getWorkflowBaseDir() + "/bin/packageGenotype.pl")
      .addArgument(OUTDIR)
      .addArgument(controlBam)
      ;
    for(String tumour : tumourBams) {
      thisJob.getCommand().addArgument(tumour);
    }
    return thisJob;
  }
  
  private Job contaminationBaseJob(int tumourCount, String inBam, String process) {
    Job thisJob = prepTimedJob(tumourCount, "verifyBamHomChk", process, 0);
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument("verifyBamHomChk.pl")
      .addArgument("-o " + OUTDIR + "/" + tumourCount + "/contamination")
      .addArgument("-b " + inBam)
      .addArgument("-d " + contamDownSampOneIn)
      .addArgument("-j " + OUTDIR + "/" + tumourCount + "/contamination/summary.json")
      ;
    if(process.equals("tumour")) {
      thisJob.getCommand().addArgument("-a " + OUTDIR + "/" + tumourCount + "/ascat/*.copynumber.caveman.csv"); // not the best approach but works
    }
    
    return thisJob;
  }
  
    private Job packageContam(List<String> tumourBams, String controlBam) {
    Job thisJob = getWorkflow().createBashJob("packageContam");
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument(getWorkflowBaseDir() + "/bin/packageContam.pl")
      .addArgument(OUTDIR)
      .addArgument(controlBam)
      ;
    for(String tumour : tumourBams) {
      thisJob.getCommand().addArgument(tumour);
    }
    return thisJob;
  }
  
  

  private Job cgpAscatBaseJob(int tumourCount, String tumourBam, String controlBam, String alg, String process, int index) {
    Job thisJob = prepTimedJob(tumourCount, alg, process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("ascat.pl")
              .addArgument("-p " + process)
              .addArgument("-i " + index)
              .addArgument("-r " + genomeFaGz)
              .addArgument("-s " + refBase + "/ascat/SnpLocus.tsv")
              .addArgument("-sp " + refBase + "/ascat/SnpPositions.tsv")
              .addArgument("-sg " + refBase + "/ascat/SnpGcCorrections.tsv")
              .addArgument("-pr " + seqType)
              .addArgument("-ra " + assembly)
              .addArgument("-rs " + species)
              .addArgument("-pl " + "ILLUMINA") // should be in BAM header
              .addArgument("-o " + OUTDIR + "/" + tumourCount + "/ascat")
              .addArgument("-t " + tumourBam)
              .addArgument("-n " + controlBam)
              .addArgument("-f") // force completion, even when ascat fails
              ;
    thisJob.getCommand().addArgument("-g " + gender);

    return thisJob;
  }

  private Job pindelBaseJob(int tumourCount, String tumourBam, String controlBam, String alg, String process, int index) {
    Job thisJob = prepTimedJob(tumourCount, alg, process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("pindel.pl")
              .addArgument("-p " + process)
              .addArgument("-r " + genomeFaGz)
              .addArgument("-e " + refExclude)
              .addArgument("-st " + seqType)
              .addArgument("-as " + assembly)
              .addArgument("-sp " + species)
              .addArgument("-s " + refBase + "/pindel/simpleRepeats.bed.gz")
              .addArgument("-f " + refBase + "/pindel/genomicRules.lst")
              .addArgument("-g " + refBase + "/vagrent/e74/Human.GRCh37.codingexon_regions.indel.bed.gz")
              .addArgument("-u " + refBase + "/pindel/pindel_np.gff3.gz")
              .addArgument("-sf " + refBase + "/pindel/softRules.lst")
              .addArgument("-b " + refBase + "/shared/ucscHiDepth_0.01_mrg1000_no_exon_coreChrs.bed.gz")
              .addArgument("-o " + OUTDIR + "/" + tumourCount + "/pindel")
              .addArgument("-t " + tumourBam)
              .addArgument("-n " + controlBam)
              ;
    if(!process.equals("pindel")) {
      thisJob.getCommand().addArgument("-i " + index);
    }
    
    if(process.equals("input")) {
      int pindelInputThreads;
      if(coresAddressable > 4) {
        pindelInputThreads = 4;
      }
      else {
        pindelInputThreads = coresAddressable;
      }
      thisJob.getCommand().addArgument("-c " + pindelInputThreads);
      thisJob.setThreads(pindelInputThreads);
    }
    return thisJob;
  }
  
  private Job markTime(String name, String item) {
    String timeFile = TIMEDIR + "/" + name + "_" + item;
    Job thisJob = getWorkflow().createBashJob("mark_" + item);
    thisJob.getCommand().addArgument("date +%s > " + timeFile);
    return thisJob;
  }
  
  private Job stageTbi() {
    Job thisJob = getWorkflow().createBashJob("getTbi");
    thisJob.getCommand()
      .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
      .addArgument(installBase)
      .addArgument(getWorkflowBaseDir() + "/bin/getTbi.pl")
      .addArgument(genomeFaGz + ".fai")
      .addArgument(tabixSrvUri)
      ;
    return thisJob;
  }

  private Job prepTimedJob(int tumourCount, String alg, String process, int index) {
    String timeFile = TIMEDIR + "/" + tumourCount + "_" + alg + "_" + process + "_" + index;
    Job thisJob = getWorkflow().createBashJob(alg + "_" + process);
    thisJob.getCommand().addArgument("/usr/bin/time /usr/bin/time --format=\"Wall_s %e\\nUser_s %U\\nSystem_s %S\\nMax_kb %M\" --output=" + timeFile);
    return thisJob;
  }
  
  private Job brassBaseJob(int tumourCount, String tumourBam, String controlBam, String alg, String process, int index) {
    Job thisJob = prepTimedJob(tumourCount, alg, process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("brass.pl")
              .addArgument("-p " + process)
              .addArgument("-g " + genomeFaGz)
              .addArgument("-e " + refExclude)
              .addArgument("-pr " + seqType)
              .addArgument("-as " + assembly)
              .addArgument("-s " + species)
              .addArgument("-pl " + "ILLUMINA") // should be in BAM header
              .addArgument("-d "  + refBase + "/shared/ucscHiDepth_0.01_mrg1000_no_exon_coreChrs.bed.gz")
              .addArgument("-r "  + refBase + "/brass/brassRepeats.bed.gz")
              .addArgument("-f "  + refBase + "/brass/brass_np.groups.gz")
              .addArgument("-g_cache "  + refBase + "/vagrent/e74/Homo_sapiens.GRCh37.74.vagrent.cache.gz")
              .addArgument("-o " + OUTDIR + "/" + tumourCount + "/brass")
              .addArgument("-t " + tumourBam)
              .addArgument("-n " + controlBam)
            ;
    if(!process.equals("assemble")) {
      thisJob.getCommand().addArgument("-i " + index);
    }
    if(process.equals("filter")) {
      String cnPath = OUTDIR + "/" + tumourCount + "/ascat/*.copynumber.caveman.csv";
      thisJob.getCommand().addArgument("-a " + cnPath);
    }
    return thisJob;
  }
  
  
  /**
   * So this isn't super useful since 1) it makes a new analysis ID on the target server
   * and 2) it overrides the workflow information since this tool doesn't have that 
   * information, needs to be a param.  The correct way to do this is to actually 
   * read the XML for this particular BAM, make a new submission by cutting up the XML
   * doc, and moving the BAM to a directory named after the analysis ID from the download server.
   * This is new tool development so I'm including this but keep in mind it's not well tested.
   * @param parentJob
   * @param bamUploadServer
   * @param bamUploadPemFile
   * @param analysisId
   * @param bamPath
   * @return 
   */
  private Job alignedBamUploadJob(Job parentJob, String bamDownloadServer, String studyRefnameOverride, String analysisCenter, String bamUploadServer, String bamUploadPemFile, String analysisId, String bamPath, String uploadScriptJobMem, String uploadScriptJobSlots) {
        Job job = this.getWorkflow().createBashJob("upload_bam");
        
        // NEED TO: make upload dir
        //          metadata URLs need to be constructed
        //          md5sum created
        
        
        // FIXME: including an echo here just to test this
        job.getCommand()
                .addArgument("echo md5sum "+bamPath+" > "+bamPath+".md5;")
                .addArgument(
                        "echo perl -I " + this.getWorkflowBaseDir() + "/bin/gt-download-upload-wrapper-1.0.0/lib " + this.getWorkflowBaseDir()
                                + "/scripts/gnos_upload_data.pl").addArgument("--bam " + bamPath)
                .addArgument("--key " + bamUploadPemFile).addArgument("--outdir bam_uploads")
                .addArgument("--metadata-urls " + bamDownloadServer + "/cghub/metadata/analysisFull/" + analysisId)
                .addArgument("--upload-url " + bamUploadServer)
                .addArgument("--study-refname-override " + studyRefnameOverride)
                .addArgument("--bam-md5sum-file " + bamPath + ".md5")
                .addArgument("--analysis-center-override " + analysisCenter)
                .addArgument("--workflow-bundle-dir " + this.getWorkflowBaseDir())
                .addArgument("--workflow-version " + this.getBundle_version())
                .addArgument("--seqware-version " + this.getSeqware_version());

        job.setMaxMemory(uploadScriptJobMem);
        job.setThreads(Integer.valueOf(uploadScriptJobSlots));
        job.addParent(parentJob);
        
        return(job);
    }

}
