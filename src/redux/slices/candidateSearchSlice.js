import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import apiService from '../../services/api';
import { logout } from './authSlice';

// Async thunk for candidate advanced search
export const performCandidateSearch = createAsyncThunk(
  'candidateSearch/search',
  async (searchParams, { rejectWithValue }) => {
    try {
      // Transform parameters to match backend DTO
      const transformedParams = {
        ...searchParams,
        // Rename skillMatchMode to primarySkillsMatchType
        primarySkillsMatchType: searchParams.skillMatchMode,
        skillMatchMode: undefined,
        // Convert numeric values to strings for backend
        minExperience: searchParams.minExperience != null ? String(searchParams.minExperience) : null,
        maxExperience: searchParams.maxExperience != null ? String(searchParams.maxExperience) : null,
        minCurrentPackage: searchParams.minCurrentPackage != null ? String(searchParams.minCurrentPackage) : null,
        maxCurrentPackage: searchParams.maxCurrentPackage != null ? String(searchParams.maxCurrentPackage) : null,
        minExpectedCTC: searchParams.minExpectedCTC != null ? String(searchParams.minExpectedCTC) : null,
        maxExpectedCTC: searchParams.maxExpectedCTC != null ? String(searchParams.maxExpectedCTC) : null
      };
      
      const response = await apiService.post('/api/candidates/advanced-search', transformedParams);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.message || 'Search failed');
    }
  }
);

const candidateSearchSlice = createSlice({
  name: 'candidateSearch',
  initialState: {
    filters: {
      textQuery: '',
      primarySkills: [],
      skillMatchMode: 'ANY',
      secondarySkills: [],
      minExperience: null,
      maxExperience: null,
      minCurrentPackage: null,
      maxCurrentPackage: null,
      minExpectedCTC: null,
      maxExpectedCTC: null,
      locations: [],
      statuses: [],
      sources: [],
      createdFrom: null,
      createdTo: null
    },
    pagination: {
      page: 0,
      size: 20,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    },
    results: {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: false,
      empty: true,
      searchTimeMs: 0
    },
    savedSearches: [],
    isSearching: false,
    error: null
  },
  reducers: {
    updateFilter: (state, action) => {
      const { field, value } = action.payload;
      state.filters[field] = value;
    },
    updateFilters: (state, action) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    resetFilters: (state) => {
      state.filters = {
        textQuery: '',
        primarySkills: [],
        skillMatchMode: 'ANY',
        secondarySkills: [],
        minExperience: null,
        maxExperience: null,
        minCurrentPackage: null,
        maxCurrentPackage: null,
        minExpectedCTC: null,
        maxExpectedCTC: null,
        locations: [],
        statuses: [],
        sources: [],
        createdFrom: null,
        createdTo: null
      };
      state.results = {
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: false,
        empty: true,
        searchTimeMs: 0
      };
    },
    changePage: (state, action) => {
      state.pagination.page = action.payload;
    },
    changeSort: (state, action) => {
      const { field, direction } = action.payload;
      state.pagination.sortBy = field;
      state.pagination.sortDirection = direction;
    },
    saveCurrentSearch: (state, action) => {
      const searchName = action.payload;
      const newSearch = {
        id: Date.now(),
        name: searchName,
        filters: { ...state.filters },
        savedAt: new Date().toISOString()
      };
      state.savedSearches.push(newSearch);
      // Save to localStorage
      localStorage.setItem('candidateSavedSearches', JSON.stringify(state.savedSearches));
    },
    loadSavedSearch: (state, action) => {
      const savedSearch = action.payload;
      state.filters = { ...savedSearch.filters };
    },
    deleteSavedSearch: (state, action) => {
      const searchId = action.payload;
      state.savedSearches = state.savedSearches.filter(s => s.id !== searchId);
      // Update localStorage
      localStorage.setItem('candidateSavedSearches', JSON.stringify(state.savedSearches));
    },
    loadSavedSearchesFromStorage: (state) => {
      const saved = localStorage.getItem('candidateSavedSearches');
      if (saved) {
        state.savedSearches = JSON.parse(saved);
      }
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(performCandidateSearch.pending, (state) => {
        state.isSearching = true;
        state.error = null;
      })
      .addCase(performCandidateSearch.fulfilled, (state, action) => {
        state.isSearching = false;
        state.results = action.payload;
      })
      .addCase(performCandidateSearch.rejected, (state, action) => {
        state.isSearching = false;
        state.error = action.payload;
      })
      // Reset search state on logout
      .addCase(logout.fulfilled, (state) => {
        state.filters = {
          textQuery: '',
          primarySkills: [],
          skillMatchMode: 'ANY',
          secondarySkills: [],
          minExperience: null,
          maxExperience: null,
          minCurrentPackage: null,
          maxCurrentPackage: null,
          minExpectedCTC: null,
          maxExpectedCTC: null,
          locations: [],
          statuses: [],
          sources: [],
          createdFrom: null,
          createdTo: null
        };
        state.results = {
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
          first: true,
          last: false,
          empty: true,
          searchTimeMs: 0
        };
        state.isSearching = false;
        state.error = null;
      });
  }
});

export const {
  updateFilter,
  updateFilters,
  resetFilters,
  changePage,
  changeSort,
  saveCurrentSearch,
  loadSavedSearch,
  deleteSavedSearch,
  loadSavedSearchesFromStorage
} = candidateSearchSlice.actions;

export default candidateSearchSlice.reducer;
