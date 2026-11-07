import axiosClient from './axiosClient';

export const weatherApi = {
  getCurrent(city = 'hcm') {
    return axiosClient.get('/api/weather', {
      params: { city },
    });
  },
};

export default weatherApi;
