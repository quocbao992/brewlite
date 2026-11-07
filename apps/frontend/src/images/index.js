import bacXiu from './bac-xiu.jpg';
import caffeLatte from './caffe-latte.jpg';
import caramelMacchiato from './caramel-macchiato.jpg';
import cfDenDa from './cf-den-da.jpg';
import cfSuaDa from './cf-sua-da.jpg';
import coldBrew from './cold-brew.jpg';
import croissant from './croissant.jpg';
import traDao from './tra-dao.jpg';

export const localImages = {
  'bac-xiu': bacXiu,
  'caffe-latte': caffeLatte,
  'caramel-macchiato': caramelMacchiato,
  'cf-den-da': cfDenDa,
  'cf-sua-da': cfSuaDa,
  'cold-brew': coldBrew,
  'croissant': croissant,
  'tra-dao': traDao,
};

export const defaultCoffeeImg = caffeLatte;

export const resolveProductImage = (product) => {
  if (!product) return defaultCoffeeImg;
  const imgURL = product.imgURL || product.img || '';
  const name = (product.name || '').toLowerCase();

  if (imgURL.startsWith('http://') || imgURL.startsWith('https://')) {
    return imgURL;
  }

  const cleanKey = imgURL
    .replace('/images/', '')
    .replace('.jpg', '')
    .replace('.png', '')
    .trim();
  if (localImages[cleanKey]) {
    return localImages[cleanKey];
  }

  if (imgURL.includes('/images/')) {
    return imgURL;
  }
  if (imgURL.endsWith('.jpg') || imgURL.endsWith('.png')) {
    return `/images/${imgURL}`;
  }

  if (name.includes('đào') || name.includes('dao')) return localImages['tra-dao'];
  if (name.includes('bạc xỉu') || name.includes('bac xiu')) return localImages['bac-xiu'];
  if (name.includes('sữa đá') || name.includes('sua da')) return localImages['cf-sua-da'];
  if (name.includes('đen đá') || name.includes('den da') || name.includes('đen')) return localImages['cf-den-da'];
  if (name.includes('cold brew') || name.includes('coldbrew') || name.includes('ủ lạnh')) return localImages['cold-brew'];
  if (name.includes('latte')) return localImages['caffe-latte'];
  if (name.includes('macchiato') || name.includes('caramel')) return localImages['caramel-macchiato'];
  if (name.includes('croissant') || name.includes('bánh') || name.includes('banh')) return localImages['croissant'];

  return defaultCoffeeImg;
};
