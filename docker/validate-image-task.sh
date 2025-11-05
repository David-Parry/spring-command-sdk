#!/bin/bash

# Most Accurate Image Validation: Compare ECR and ECS Image Digests

set -e  # Exit on error

REPOSITORY_NAME="command-sdk"
CLUSTER_NAME="command-sdk-cluster"
SERVICE_NAME="command-sdk-service"
REGION="us-east-2"

echo "=========================================="
echo "Image Digest Validation"
echo "=========================================="

# Get ECR latest image digest
echo -e "\n🔍 Fetching ECR image digest..."
ECR_DIGEST=$(aws ecr describe-images \
  --repository-name $REPOSITORY_NAME \
  --image-ids imageTag=latest \
  --region $REGION \
  --query 'imageDetails[0].imageDigest' \
  --output text)

if [ -z "$ECR_DIGEST" ]; then
  echo "❌ ERROR: Could not fetch ECR image digest"
  exit 1
fi

echo "ECR Digest: $ECR_DIGEST"

# Get running task ARN
echo -e "\n🔍 Fetching ECS task information..."
TASK_ARN=$(aws ecs list-tasks \
  --cluster $CLUSTER_NAME \
  --service-name $SERVICE_NAME \
  --region $REGION \
  --query 'taskArns[0]' \
  --output text)

if [ -z "$TASK_ARN" ] || [ "$TASK_ARN" == "None" ]; then
  echo "❌ ERROR: No running tasks found"
  exit 1
fi

echo "Task ARN: $TASK_ARN"

# Get ECS task image digest
ECS_DIGEST=$(aws ecs describe-tasks \
  --cluster $CLUSTER_NAME \
  --tasks $TASK_ARN \
  --region $REGION \
  --query 'tasks[0].containers[0].imageDigest' \
  --output text)

if [ -z "$ECS_DIGEST" ]; then
  echo "❌ ERROR: Could not fetch ECS task image digest"
  exit 1
fi

echo "ECS Digest: $ECS_DIGEST"

# Compare digests
echo -e "\n=========================================="
if [ "$ECR_DIGEST" == "$ECS_DIGEST" ]; then
  echo "✅ VALIDATION PASSED"
  echo "Task is using the latest image from ECR"
  exit 0
else
  echo "❌ VALIDATION FAILED"
  echo "Task is using an older image"
  echo ""
  echo "Expected (ECR):  $ECR_DIGEST"
  echo "Actual (ECS):    $ECS_DIGEST"
  exit 1
fi