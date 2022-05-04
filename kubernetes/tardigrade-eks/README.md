# Testing Trino Tardigrade Features using Amazon EKS

## Introduction 
If you are new to Trino, this tutorial is a rather in depth topic. We recommend you read the [Intro to Trino for the Trinewbie](https://medium.com/geekculture/intro-to-trino-for-the-trinewbie-a5a1088d3114) blog to better understand Trino then read the [Project Tardigrade Launch](https://trino.io/blog/2022/05/05/tardigrade-launch.html) blog to learn more about the features that have been added.

<p align="center" width="100%">
    <img width="50%" src="tardigrade-logo.png">
</p>

## Goals
In this tutorial, you will:
 1. Learn how to set up a Trino cluster with sufficient size to test the new Project Tardigrade features.
 2. Learn how to quickly provision Amazon EKS and S3 resources using helm charts and eksctl.
 3. Do some basic querying of the TPCH and TPCDS benchmark queries.

### Video Tutorial

[![Watch the video](youtube-trino.png)](https://youtu.be/4isawxYjDnE)
 
## Steps

### Install all the required tools.

* [aws cli](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)([about](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-welcome.html))
* [eksctl](https://eksctl.io/introduction/#installation)([about](https://eksctl.io))
* [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)([about](https://kubernetes.io/docs/reference/kubectl/kubectl/))
* [helm](https://helm.sh/docs/intro/install/)([about](https://helm.sh))

To verify if these are installed and which version run the following commands.
```
aws --version
eksctl version
kubectl version
helm version
```

### Create an AWS IAM User in the AWS Console

Trino Tardigrade depends on a buffer layer that is currently implemented using an S3 compabtible storage service. In this example, we'll use AWS S3 and will need to have a user that configures the S3 bucket to store intermediate query results. This user will also be needed to deploy Trino on the EKS system. Follow the [documentation on AWS to create an IAM User](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_users_create.html#id_users_create_console). This user must have full access to S3 buckets specified for spooling and for permanent storage as well as access to `Glue`. For the EKS set up, this user must also have the [IAM policies required by eksctl](https://eksctl.io/usage/minimum-iam-policies/) in order to work. In Total, the following AWS managed policies are required:

* AmazonEC2FullAccess
* AWSCloudFormationFullAccess
* IAMFullAccess
* AmazonS3FullAccess
* AWSGlueConsoleFullAccess
* EKSAllAccess (Note: Non-AWS Managed Policy that requires you to update [account id](https://docs.aws.amazon.com/IAM/latest/UserGuide/console_account-alias.html))
```
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "eks:*",
            "Resource": "*"
        },
        {
            "Action": [
                "ssm:GetParameter",
                "ssm:GetParameters"
            ],
            "Resource": [
                "arn:aws:ssm:*:<account_id>:parameter/aws/*",
                "arn:aws:ssm:*::parameter/aws/*"
            ],
            "Effect": "Allow"
        },
        {
             "Action": [
               "kms:CreateGrant",
               "kms:DescribeKey"
             ],
             "Resource": "*",
             "Effect": "Allow"
        },
        {
             "Action": [
               "logs:PutRetentionPolicy"
             ],
             "Resource": "*",
             "Effect": "Allow"
        }        
    ]
}
```


Once you have created the IAM user, the IAM service will generate an access key to identify the user, and a secret key that acts as a password. The secret key is available only upon creation so make sure to save it. An example of what these look like is `AKIAIOSFODNN7EXAMPLE` and `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`. With these keys and the region you are running in, run the following config command in the aws cli. 

```
$ aws configure
```

For more information, read the documentation about [how to configure your keys for the cli](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html#cli-configure-files-methods).

### Create AWS S3 bucket

To create your spool bucket in S3, you'll need to create a [bucket name that is unique across the AWS regions in your partition](https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html). The easiest way to do this is create a UUID with prefix `tardigrade-spool` and `tardigrade-data`. For example, we would run the following command to create the bucket using a UUID generated in python.

```
TARDIGRADE_UUID=$(python -c 'import uuid; print(uuid.uuid4())')
aws s3 mb "s3://tardigrade-spool-${TARDIGRADE_UUID}"
aws s3 mb "s3://tardigrade-data-${TARDIGRADE_UUID}"
```

Now we need to configure the lifecycle bucket policy to expire abandoned objects in the spooler:

```
aws s3api put-bucket-lifecycle-configuration --bucket "tardigrade-spool-${TARDIGRADE_UUID}" --cli-input-json file://spool-lifecycle-rules.json
```

### Create a `values.yaml` file 

Create a `values.yml` file by first copying the `values-template.yaml` to a new file `values.yml` and relacing placeholders in your `values.yaml`:
   * `<trino-version>` - latest release of Trino, e.g.: `379`
   * `<number-of-workers>` - desired number of workers, e.g.: `5`
   * `<spool-bucket-name>` - name of a bucket to be used for spooling, e.g.: `s3://tardigrade-spool-${TARDIGRADE_UUID}`
     Multiple buckets can be specified using comma,
     e.g.: `s3://bucket-1,s3://bucket-2`
   * `<aws-region>` - aws region where the cluster is being deployed,
     e.g.: `us-east-1`
   * `<permanent-data-bucket>` - name of a bucket to be used for storing tables
     created by Trino, e.g.: `s3://tardigrade-data-${TARDIGRADE_UUID}`
   * `<access-key>`, `<secret-key>` - AWS access credentials. 

### Run EKS clusters

With the AWS account and buckets created and the values.yaml set up with all of the placeholders filled in, it's time to run the EKS cluster deployment. Per the suggestions provided by the Tardigrade team on the blog, they recommend at least 1 coordinator node and 5 worker nodes, each of size `m5.8xlarge`. Use the following command to create the cluster.

```
 eksctl create cluster \
   --name tardigrade-cluster \
   --region us-east-1 \
   --node-type m5.8xlarge \
   --nodes 6
```

### Install Trino coordinator and worker using the Trino Helm Chart and Kubernetes.

Notice: This uses the `values.yaml` you just created.

If you haven't yet, add the Trino helm repository with the following command:

```
helm repo add trino https://trinodb.github.io/charts/
```

Now install Trino on the EKS cluster using Helm.

```
helm upgrade --install \
  --values values.yaml \
  tardigrade-kubernetes-cluster \
  trino/trino \
  --version 0.7.0
```

Now do a quick restart to make sure all nodes got the correct settings loaded.

```
kubectl rollout restart deployment \
  tardigrade-kubernetes-cluster-trino-coordinator \
  tardigrade-kubernetes-cluster-trino-worker
```

### Create a tunnel from the kubernetes cluster to your local laptop.

First, you'll need to forward the kubernetes port locally by running the following: 
```
kubectl port-forward \
$(kubectl get pods --namespace default -l "app=trino,release=tardigrade-kubernetes-cluster,component=coordinator" -o jsonpath="{.items[0].metadata.name}") \
8080:8080
```

### Download the Trino CLI.

Download the [Trino CLI](https://trino.io/docs/current/installation/cli.html) or any other client that can run [a JDBC Driver](https://trino.io/docs/current/installation/jdbc.html) and point it to the local server at `127.0.0.1:8080` (This is the default url in the cli). 

### Run some TPCH and TPCDS queries.

Congratutulations! You have now set up your first Tardigrade cluster! Now it's time to test the features! We're going to borrow the [TPCH](https://github.com/trinodb/trino-verifier-queries/tree/main/src/main/resources/queries/tpch/etl) and [TPCDS](https://github.com/trinodb/trino-verifier-queries/tree/main/src/main/resources/queries/tpcds/etl) queries to run a long running query, then remove a node to cause it to fail.

```
CREATE SCHEMA hive.sf10
WITH ( location = 's3://tardigrade-data-4d0faa5d-5f92-4b6c-8c18-fef6d359a28b/sf10/');
```

```
CREATE TABLE hive.sf10.q1
AS
WITH
  customer_total_return AS (
   SELECT
     "sr_customer_sk" "ctr_customer_sk"
   , "sr_store_sk" "ctr_store_sk"
   , "sum"("sr_return_amt") "ctr_total_return"
   FROM
     store_returns
   , date_dim
   WHERE ("sr_returned_date_sk" = "d_date_sk")
   GROUP BY "sr_customer_sk", "sr_store_sk"
)
SELECT *
FROM
  customer_total_return ctr1
, store
, customer
WHERE ("ctr1"."ctr_total_return" <> (
      SELECT ("avg"("ctr_total_return") * DECIMAL '1.2')
      FROM
        customer_total_return ctr2
      WHERE ("ctr1"."ctr_store_sk" = "ctr2"."ctr_store_sk")
   ))
   AND ("s_store_sk" = "ctr1"."ctr_store_sk")
   AND ("ctr1"."ctr_customer_sk" = "c_customer_sk")

```

While this is running, run the following command to scale down the number of worker nodes to crash one of the tasks manually.

```
kubectl scale deployment tardigrade-kubernetes-cluster-trino-worker --replicas=4
```


### Tear down S3 an EKS cluster

To remove the S3 buckets and all the data in them, run the following commands:

```
aws s3 rb "s3://tardigrade-spool-${TARDIGRADE_UUID}" --force
aws s3 rb "s3://tardigrade-data-${TARDIGRADE_UUID}" --force
```

To tear down the Tardigrade EKS cluster, run the following command:

```
eksctl delete cluster \
  --name tardigrade-cluster \
  --region us-east-1
```

## Potential issues

* Queries may start failing with
  `software.amazon.awssdk.services.s3.model.S3Exception: Please reduce your request rate`:
  * If your workload is I/O intensive it is possible that S3 starts throttling
    some requests. The request limits are enforced per bucket. It is
    recommended to create an another bucket for spooling to allow
    Trino to balance the load
    `exchange.base-directories=s3://<first-bucket>,s3://<second-bucket>`
* Queries may fail with `Task descriptor storage capacity has been exceeded`:
  * Trino has to maintain descriptors for each task in case it has to be
    restarted in an event of a failure. This information is currently stored
    in memory on the coordinator. We are planning to implement
    adaptive spilling for task descriptors, but at the moment there is a
    chance that queries may hit this limit. If this happens we recommend that
    you run less queries concurrently or use a coordinator with more memory.

See trademark and other [legal notices](https://trino.io/legal.html).
