import { S3Client, GetObjectCommand, PutObjectCommand } from '@aws-sdk/client-s3';
import sharp from 'sharp';

const s3 = new S3Client();
const THUMB_WIDTH = 400;

export const handler = async (event) => {
  const bucket = event.Records[0].s3.bucket.name;
  const key = decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, ' '));

  // サムネイルは処理しない（無限ループ防止）
  if (key.includes('_thumb')) return;

  // 画像のみ処理
  const ext = key.split('.').pop().toLowerCase();
  if (!['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) return;

  console.log(`Processing thumbnail for: ${key}`);

  const { Body, ContentType } = await s3.send(new GetObjectCommand({ Bucket: bucket, Key: key }));
  const buffer = Buffer.from(await Body.transformToByteArray());

  const thumbnail = await sharp(buffer)
    .resize(THUMB_WIDTH)
    .toBuffer();

  const thumbKey = key.replace(/(\.[^.]+)$/, '_thumb$1');
  await s3.send(new PutObjectCommand({
    Bucket: bucket,
    Key: thumbKey,
    Body: thumbnail,
    ContentType
  }));

  console.log(`Thumbnail created: ${thumbKey}`);
};
