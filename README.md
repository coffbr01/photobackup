Given a photoprism instance, back up all months as archive.zip to an S3 bucket.

### Required environment variables

# The photoprism host to pull albums from
export PBKP_HOST=the.photoprism.host

# The photoprism username
export PBKP_USER=thePhotoprismUser

# The photoprism password
export PBKP_PASS=thePhotoprismPassword

# The S3 bucket to upload the album archives to
export PBKP_BUCKET=the-amazon-s3-bucket

# The S3 storage class. e.g. STANDARD, DEEP_ARCHIVE, etc. See software.amazon.awssdk.services.s3.model.StorageClass
export PBKP_STORAGE_CLASS=THE_STORAGE_CLASS
