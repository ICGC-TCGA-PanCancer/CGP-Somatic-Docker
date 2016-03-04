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
public class CgpSomaticCore extends AbstractWorkflowDataModel {

  private static String OUTDIR = "outdir";
  private static String TIMEDIR;
  private static String COUNTDIR;
  private static String BBDIR;
  private boolean cleanup = false;
  private boolean cleanupBams = false;
  
  // datetime all upload files will be named with
  DateFormat df = new SimpleDateFormat("yyyyMMdd");
  String dateString = df.format(Calendar.getInstance().getTime());

  private String workflowName = Version.WORKFLOW_SHORT_NAME_VERSION;
  
  // MEMORY variables //
  private String  memGenerateBasFile, memPackageResults, memMarkTime,
                  memQcMetrics, memGenotype, memContam,
                  memBbMerge,
                  // ascat memory
                  memAlleleCount, memAscat, memAscatFinalise,
                  // pindel memory
                  memPindelInput, memPindelPerThread, memPindelVcf, memPindelMerge , memPindelFlag,
                  // brass memory
                  memBrassInput, memBrassCoverPerThread, memBrassCoverMerge, memBrassGroup, memBrassIsize,
                  memBrassNormCn, memBrassFilter, memBrassSplit,
                  memBrassAssemblePerThread, memBrassGrass, memBrassTabix,
                  // caveman memory
                  memCaveCnPrep,
                  memCavemanSetup, memCavemanSplit, memCavemanSplitConcat,
                  memCavemanMstepPerThread, memCavemanMerge, memCavemanEstepPerThread,
                  memCavemanMergeResults, memCavemanAddIds, memCavemanFlag
          ;

  // workflow variables
  private String  // reference variables
                  species, assembly, refFrom, bbFrom,
                  // sequencing type/protocol
                  seqType, seqProtocol,
                  //GNOS identifiers
                  pemFile, uploadPemFile, gnosServer, uploadServer,
                  // ascat variables
                  gender,
                  // pindel variables
                  refExclude, pindelGermline,
                  //general variables
                  installBase, refBase, genomeFa, testBase,
                  //contamination variables
                  contamDownSampOneIn
                  ;

  private int coresAddressable, memWorkflowOverhead, memHostMbAvailable;
  
  // UUID
  private String uuid = UUID.randomUUID().toString().toLowerCase();

  // if localFileMode, this is the path at which the workflow will find the XML files used for metadata in the upload of VCF
//  private String localXMLMetadataPath = null;
//  private String localBamFilePathPrefix = null;
  
  // used for downloading from S3
//  private boolean downloadBamsFromS3 = false;
//  private String normalS3Url = "";
//  private ArrayList<String> tumorS3Urls = null;
//  private String S3DownloadKey = "";
//  private String S3DownloadSecretKey = "";

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
      
      if(hasPropertyAndNotNull("cleanup")) {
        cleanup = Boolean.valueOf(getProperty("cleanup"));
      }
      
      if(hasPropertyAndNotNull("cleanupBams")) {
        cleanupBams = Boolean.valueOf(getProperty("cleanupBams"));
      }
      
      // used by steps that can use all available cores
      coresAddressable = Integer.valueOf(getProperty("coresAddressable"));

      // MEMORY //
      memGenerateBasFile = getProperty("memGenerateBasFile");
      memPackageResults = getProperty("memPackageResults");
      memMarkTime = getProperty("memMarkTime");
      memQcMetrics = getProperty("memQcMetrics");
      memGenotype = getProperty("memGenotype");
      memContam = getProperty("memContam");
      
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
      memBrassCoverPerThread = getProperty("memBrassCoverPerThread");
      memBrassCoverMerge = getProperty("memBrassCoverMerge");
      memBrassGroup = getProperty("memBrassGroup");
      memBrassIsize = getProperty("memBrassIsize");
      memBrassNormCn = getProperty("memBrassNormCn");
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
      
      memWorkflowOverhead = Integer.valueOf(getProperty("memWorkflowOverhead"));
      memHostMbAvailable = Integer.valueOf(getProperty("memHostMbAvailable"));
      
      contamDownSampOneIn = getProperty("contamDownSampOneIn");

      // REFERENCE INFO //
      species = getProperty("species");
      assembly = getProperty("assembly");
      refFrom = getProperty("refFrom");
      bbFrom = getProperty("bbFrom");
      
      // Sequencing info
      seqType = getProperty("seqType");
      if(seqType.equals("WGS")) {
        seqProtocol = "genomic";
      }

      // Specific to ASCAT workflow //
      gender = getProperty("gender");
      
      // pindel specific
      refExclude = getProperty("refExclude");

      //environment
      installBase = "/opt/wtsi-cgp";
      refBase = OUTDIR + "/reference_files";
      genomeFa = refBase + "/genome.fa";
      
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    
    return getFiles();
  }
  
  @Override
  public void buildWorkflow() {
    Job startDownload = markTime("workflow", "start");
    startDownload.setMaxMemory(memMarkTime);
    
    try {
      
      List<String> rawBams = Arrays.asList(getProperty("tumourBams").split(":"));
      if(rawBams.size() == 0) {
        throw new RuntimeException("Propertie tumourBams has no list of BAM files");
      }
      List<String> tumourBams = new ArrayList<String>();
      int tumBamCount = rawBams.size();
      for(int i=0; i<tumBamCount; i++) {
        String tumourBam = rawBams.get(i);
        tumourBams.add(tumourBam);
      }
      
      String controlBam = getProperty("controlBam");
      
      Job genotypeJob = genoptypeBaseJob(tumourBams, controlBam);
      genotypeJob.setMaxMemory(memGenotype);

      Job genotypePackJob = packageGenotype(tumourBams, controlBam);
      genotypePackJob.setMaxMemory("4000");
      genotypePackJob.addParent(genotypeJob);

      Job contaminationJob = contaminationBaseJob(tumBamCount, controlBam, "control");
      contaminationJob.setMaxMemory(memContam);
      
      String tmpRef = OUTDIR + "/" + "ref.tar.gz";
      Job pullRef = pullRef(refFrom, tmpRef);
      pullRef.addParent(startDownload);
      pullRef.setMaxMemory(memMarkTime);

      String tmpBbRef = OUTDIR + "/" + "bb.tar.gz";
      Job pullBbRef = pullRef(bbFrom, tmpBbRef);
      pullBbRef.addParent(startDownload);
      pullBbRef.setMaxMemory(memMarkTime);

      Job unpackRef = unpackRef(tmpRef, null);
      unpackRef.addParent(pullRef);
      unpackRef.setMaxMemory(memMarkTime);

      Job unpackBbRef = unpackRef(tmpBbRef, "reference_files");
      unpackBbRef.addParent(pullBbRef);
      unpackBbRef.addParent(unpackRef);
      unpackBbRef.setMaxMemory(memMarkTime);
      
      
      List<Job> basJobsList = new ArrayList<Job>();
      
      // @todo need to add code to generate BAS for BAM
      
      Job controlBasJob = basFileBaseJob(0, controlBam, "control", 0);
      controlBasJob.setMaxMemory(memGenerateBasFile);
      controlBasJob.addParent(unpackRef);
      basJobsList.add(controlBasJob);
      
      for(int i=0; i<tumBamCount; i++) {
        String tumourBam = rawBams.get(i);
        Job tumourBasJob = basFileBaseJob(tumBamCount, tumourBam, "tumours", i+1);
        tumourBasJob.setMaxMemory(memGenerateBasFile);
        tumourBasJob.addParent(unpackRef);
        basJobsList.add(tumourBasJob);
      }

      // packaging must have parent cavemanTbiCleanJob

      // these are not paired but per individual sample
      List<Job> bbAlleleCountJobs = new ArrayList<Job>();
      for(int i=0; i<23; i++) { // not 1-22+X
        for(int j=0; j<tumBamCount; j++) {
          Job bbAlleleCountJob = bbAlleleCount(j, tumourBams.get(j), "tumour", i);
          bbAlleleCountJob.setMaxMemory(memAlleleCount);
          addJobParents(bbAlleleCountJob, basJobsList);
          bbAlleleCountJobs.add(bbAlleleCountJob);
        }
        Job bbAlleleCountJob = bbAlleleCount(1, controlBam, "control", i);
        bbAlleleCountJob.setMaxMemory(memAlleleCount);
        addJobParents(bbAlleleCountJob, basJobsList);
        bbAlleleCountJobs.add(bbAlleleCountJob);
      }

      Job bbAlleleMergeJob = bbAlleleMerge(controlBam);
      bbAlleleMergeJob.setMaxMemory(memBbMerge);
      for(Job j : bbAlleleCountJobs) {
        bbAlleleMergeJob.addParent(j);
      }

      // donor based workflow section
      Job[] cavemanFlagJobs = new Job [tumBamCount];
      for(int i=0; i<tumBamCount; i++) {
        Job cavemanFlagJob = buildPairWorkflow(basJobsList, controlBam, tumourBams.get(i), i);
        cavemanFlagJobs[i] = cavemanFlagJob;
      }

      Job endWorkflow = markTime("workflow", "end");
      endWorkflow.setMaxMemory(memMarkTime);
      for(Job cavemanFlagJob : cavemanFlagJobs) {
        endWorkflow.addParent(cavemanFlagJob);
      }

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
      for(Job cavemanFlagJob : cavemanFlagJobs) {
        packageContamJob.addParent(cavemanFlagJob);
      }

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
      
      // delete just the BAM inputs and not the output dir
      if (cleanup || cleanupBams) {
        Job cleanInputsJob = cleanJob();
        cleanInputsJob.addParent(metricsJob);
      }
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
  
    /**
   * This builds the workflow for a pair of samples
   * The generic buildWorkflow section will choose the pair to be processed and 
   * setup the control sample download
   */
  private Job buildPairWorkflow(List downloadJobsList, String controlBam, String tumourBam, int tumourCount) {
    
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
      addJobParents(alleleCountJob, downloadJobsList);
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
      addJobParents(caveCnPrepJob, downloadJobsList);
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
      // If you tell SGE you are using ,multiple cores it multiplies the requested memory for you
      inputParse.setMaxMemory( memPindelInput );
      addJobParents(inputParse, downloadJobsList);
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
      addJobParents(brassInputJob, downloadJobsList);
      brassInputJobs[i] = brassInputJob;
    }
    
    int brassCoverNormalisedThreads = getMemNormalisedThread(memBrassCoverPerThread, coresAddressable);
    int totalBrassCoverMem = Integer.valueOf(memBrassCoverPerThread) + (Integer.valueOf(memWorkflowOverhead) / brassCoverNormalisedThreads);
    
    Job brassCoverJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "cover", 1);
    brassCoverJob.getCommand().addArgument("-l " + brassCoverNormalisedThreads);
    brassCoverJob.getCommand().addArgument("-c " + brassCoverNormalisedThreads);
    brassCoverJob.setMaxMemory(Integer.toString(totalBrassCoverMem));
    brassCoverJob.setThreads(brassCoverNormalisedThreads);
    addJobParents(brassCoverJob, downloadJobsList);
    
    Job brassCoverMergeJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "merge", 1);
    brassCoverMergeJob.setMaxMemory(memBrassCoverMerge);
    brassCoverMergeJob.addParent(brassCoverJob);
    brassCoverMergeJob.addParent(brassInputJobs[0]);
    brassCoverMergeJob.addParent(brassInputJobs[1]);
    
    Job brassGroupJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "group", 1);
    brassGroupJob.setMaxMemory(memBrassGroup);
    brassGroupJob.addParent(brassCoverMergeJob);
    
      Job brassIsizeJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "isize", 1);
    brassIsizeJob.setMaxMemory(memBrassIsize);
    brassIsizeJob.addParent(brassCoverMergeJob);
    
      Job brassNormCnJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "normcn", 1);
    brassNormCnJob.setMaxMemory(memBrassNormCn);
    brassNormCnJob.addParent(brassCoverMergeJob);
    
    Job brassFilterJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "filter", 1);
    brassFilterJob.setMaxMemory(memBrassFilter);
    brassFilterJob.addParent(brassGroupJob);
    brassFilterJob.addParent(brassIsizeJob);
    brassFilterJob.addParent(brassNormCnJob);
    brassFilterJob.addParent(ascatFinaliseJob); // NOTE: dependency on ASCAT!!
    
    Job brassSplitJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "split", 1);
    brassSplitJob.setMaxMemory(memBrassSplit);
    brassSplitJob.addParent(brassFilterJob);

    
    int brassAssNormalisedThreads = getMemNormalisedThread(memBrassAssemblePerThread, coresAddressable);
    int totalBrassAssMem = Integer.valueOf(memBrassAssemblePerThread) + (Integer.valueOf(memWorkflowOverhead) / brassAssNormalisedThreads);
    
    Job brassAssembleJob = brassBaseJob(tumourCount, tumourBam, controlBam, "BRASS", "assemble", 1);
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
    addJobParents(cavemanFlagJob, downloadJobsList);
    cavemanFlagJob.addParent(pindelFlagJob); // PINDEL dependency
    cavemanFlagJob.addParent(cavemanAddIdsJob);
    cavemanFlagJob.addParent(contaminationJob);
    
    Job cavemanPackage = packageResults(tumourCount, "caveman", "snv_mnv", tumourBam, "flagged.muts.vcf.gz", workflowName, "somatic", dateString);
    cavemanPackage.setMaxMemory(memPackageResults);
    cavemanPackage.addParent(cavemanFlagJob);
    
    return cavemanFlagJob;
  }
  
  private void addJobParents(Job child, List<Job> parents) {
    for(int i=0; i<parents.size(); i++) {
      child.addParent(parents.get(i));
    }
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
    
    if((memoryAvail / threads) >= Integer.valueOf(perThreadMemory)) {
      usableThreads = threads;
    }
    else {
      usableThreads = memoryAvail / Integer.valueOf(perThreadMemory);
    }
    
    if(usableThreads == 0) {
      throw new RuntimeException("memHostMbAvailable - memWorkflowOverhead = memoryAvail (" +
                                memHostMbAvailable + " - " + memWorkflowOverhead + " = " + memoryAvail +
                                ") is less than one of the mem*PerThread parameters in provided ini file.");
    }
    
    return usableThreads;
  }
  
  private Job basFileBaseJob(int tumourCount, String sampleBam, String process, int index) {
    Job thisJob = prepTimedJob(tumourCount, "basFileGenerate", process, index);
    thisJob.getCommand()
            .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
            .addArgument(installBase)
            .addArgument("bam_stats")
            .addArgument("-i " + sampleBam)
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
  
  private Job cleanJob() {
    Job thisJob = getWorkflow().createBashJob("GeneralCleanup");
    // this just removes the contents of the working directory and not OUTDIR which may point to another filesystem for archival purposes
    if (cleanupBams) {
      thisJob.getCommand().addArgument("rm -f ./*/*.bam; ");
    }
    // this removes the whole working directory
    if (cleanup) {
      thisJob.getCommand().addArgument("rm -rf *; ");
    }
    // cleans up the downloaded reference
    thisJob.getCommand().addArgument("rm -rf " + OUTDIR + "/reference_files");
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
              .addArgument("-r " + genomeFa + ".fai")
              .addArgument("-u " + refBase + "/caveman");
    
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
              .addArgument("-r " + genomeFa)
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
              .addArgument("-r " + genomeFa)
              .addArgument("-e " + refExclude)
              .addArgument("-st " + seqType)
              .addArgument("-as " + assembly)
              .addArgument("-sp " + species)
              .addArgument("-s " + refBase + "/pindel/simpleRepeats.bed.gz")
              .addArgument("-f " + refBase + "/pindel/genomicRules.lst")
              .addArgument("-g " + refBase + "/pindel/human.GRCh37.indelCoding.bed.gz")
              .addArgument("-u " + refBase + "/pindel/pindel_np.gff3.gz")
              .addArgument("-sf " + refBase + "/pindel/softRules.lst")
              .addArgument("-b " + refBase + "/brass/ucscHiDepth_0.01_mrg1000_no_exon_coreChrs.bed.gz")
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
  
  private Job prepTimedJob(int tumourCount, String alg, String process, int index) {
    String timeFile = TIMEDIR + "/" + tumourCount + "_" + alg + "_" + process + "_" + index;
    Job thisJob = getWorkflow().createBashJob(alg + "_" + process);
    thisJob.getCommand().addArgument("/usr/bin/time /usr/bin/time --format=\"Wall_s %e\\nUser_s %U\\nSystem_s %S\\nMax_kb %M\" --output=" + timeFile);
    return thisJob;
  }
  
  private Job pullRef(String refFrom, String localTarGzFile) {
    Job thisJob = prepTimedJob(0, "pullRef", "NA", 0);
    thisJob.getCommand().addArgument("curl -sSL -o " + localTarGzFile + " " + refFrom);
    return thisJob;
  }
  
  private Job unpackRef(String localTarGzFile, String suffixPath) {
    String changeTo = OUTDIR;
    if(suffixPath != null) {
      changeTo = OUTDIR + "/" + suffixPath;
    }
    Job thisJob = prepTimedJob(0, "unpackRef", "NA", 0);
    thisJob.getCommand().addArgument("tar -C " + changeTo + " -zxf " + localTarGzFile)
                        .addArgument("; rm -rf " + localTarGzFile);
    return thisJob;
  }
  
  
  private Job brassBaseJob(int tumourCount, String tumourBam, String controlBam, String alg, String process, int index) {
    
    String cnPath = OUTDIR + "/" + tumourCount + "/ascat/*.copynumber.caveman.csv";
    String cnStats = OUTDIR + "/" + tumourCount + "/ascat/*.samplestatistics.csv";
    
    Job thisJob = prepTimedJob(tumourCount, alg, process, index);
    thisJob.getCommand()
              .addArgument(getWorkflowBaseDir()+ "/bin/wrapper.sh")
              .addArgument(installBase)
              .addArgument("brass.pl")
              .addArgument("-j 4 -k 4")
              .addArgument("-p " + process)
              .addArgument("-g " + genomeFa)
              .addArgument("-e " + refExclude)
              .addArgument("-pr " + seqType)
              .addArgument("-as " + assembly)
              .addArgument("-s " + species)
              .addArgument("-pl " + "ILLUMINA") // should be in BAM header
              .addArgument("-d "  + refBase + "/brass/ucscHiDepth_0.01_mrg1000_no_exon_coreChrs.bed.gz")
              .addArgument("-f "  + refBase + "/brass/brass_np.groups.gz")
              .addArgument("-g_cache "  + refBase + "/vagrent/e75/Homo_sapiens.GRCh37.75.vagrent.cache.gz")
              .addArgument("-o " + OUTDIR + "/" + tumourCount + "/brass")
              .addArgument("-t " + tumourBam)
              .addArgument("-n " + controlBam)
              .addArgument("-vi " + refBase + "/brass/viral.1.1.genomic.fa")
              .addArgument("-mi " + refBase + "/brass/all_ncbi_bacteria.20150703")
              .addArgument("-b " + refBase + "/brass/hs37d5_500bp_windows.gc.bed.gz")
            ;
    if(process.equals("normcn") || process.equals("filter") || process.equals("grass")) {
      thisJob.getCommand().addArgument("-a " + cnPath)
                          .addArgument("-ss " + cnStats);
    }
    return thisJob;
  }
}
