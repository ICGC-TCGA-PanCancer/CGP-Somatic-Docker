task Seqware_Sanger_Somatic_Workflow {
    File tumor
    File normal
    File refFrom
    File bbFrom
    String outputDir

    command {
        python /home/seqware/CGP-Somatic-Docker/scripts/run_seqware_workflow.py \
        --tumor ${tumor} \
        --normal ${normal} \
        --refFrom ${refFrom} \
        --bbFrom ${bbFrom}
        --output-dir ${outputDir}
    }

    output {
        Array[File] somatic_snv_mnv_vcf = glob('${outputDir}*.somatic.snv_mnv.vcf.gz')
        Array[File] somatic_snv_mnv_vcf_tbi = glob('${outputDir}*.somatic.snv_mnv.vcf.gz.tbi')
        Array[File] somatic_snv_mnv_tar_gz = glob('${outputDir}*.somatic.snv_mnv.tar.gz')
        Array[File] somatic_cnv_vcf = glob('${outputDir}*.somatic.cnv.vcf.gz')
        Array[File] somatic_cnv_vcf_tbi = glob('${outputDir}*.somatic.cnv.vcf.gz.tbi')
        Array[File] somatic_cnv_tar_gz = glob('${outputDir}*.somatic.cnv.tar.gz')
        Array[File] somatic_sv_vcf = glob('${outputDir}*.somatic.sv.vcf.gz')
        Array[File] somatic_sv_vcf_tbi = glob('${outputDir}*.somatic.sv.vcf.gz.tbi')
        Array[File] somatic_sv_tar_gz = glob('${outputDir}*.somatic.sv.tar.gz')
        Array[File] somatic_indel_vcf = glob('${outputDir}*.somatic.indel.vcf.gz')
        Array[File] somatic_indel_vcf_tbi = glob('${outputDir}*.somatic.indel.vcf.gz.tbi')
        Array[File] somatic_indel_tar_gz = glob('${outputDir}*.somatic.indel.tar.gz')
        Array[File] somatic_imputeCounts_tar_gz = glob('${outputDir}*.somatic.imputeCounts.tar.gz')
        Array[File] somatic_genotype_tar_gz = glob('${outputDir}*.somatic.genotype.tar.gz')
        Array[File] somatic_verifyBamId_tar_gz = glob('${outputDir}*.somatic.verifyBamId.tar.gz')
    }

    runtime {
        docker: 'quay.io/TBD'
    }
}

workflow Seqware_Sanger_Somatic_Workflow {
    call Seqware_Sanger_Somatic_Workflow
}
