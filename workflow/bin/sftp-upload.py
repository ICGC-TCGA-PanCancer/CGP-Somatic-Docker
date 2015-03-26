import os
import pexpect
import sys

# Pexpect based sftp uploader for vcf files on bionimbus
# Drives transfers via sftp command line - simple stuff
# Requires installation of pexpect (pip install pexpect)

PROMPT='sftp'
SERVER='tcgaftps.nci.nih.gov'
USER='ByrneN'

def upload(path, uuid):
    os.chdir(path)
    p = pexpect.spawn('sftp %s@%s' % (USER, SERVER))
    p.expect('password:', timeout=120)
    p.send(sys.argv[1]+'\n')
    p.expect(PROMPT, timeout=120)
    print "LOGGED IN"
    p.send('cd pancan/variant_calling_pilot_64/OICR_Sanger_Core\n')
    p.expect(PROMPT, timeout=120)
    print "MOVED TO FOLDER"
    for filename in os.listdir(path):
        if 'somatic' in filename or 'germline' in filename:
                print "Uploading: %s" % (filename)
                p.send('put %s' % (filename) + '\n')
                cache=""
                while 1:
                        p.expect('(.*)', timeout=1000)
                        oldcache = cache
                        cache = cache + p.match.group(1)
                        if cache != oldcache:
                                print cache
                        if PROMPT in cache:
                                break
    p.close()

def main():

    if len(sys.argv) < 3:
        print "USAGE: sftp_upload.py password folder"
        sys.exit(1)

    os.system("du -Lh %s/seqware-results/upload/*" % (sys.argv[2]))
    upload_uuid = raw_input("Copy and Paste the path to use: ")

    upload(upload_uuid, sys.argv[2])

if __name__ == '__main__':
    main()
