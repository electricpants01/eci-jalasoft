package eci.technician.interfaces

interface ISearchableList {

    fun setupRecycler()
    fun setupSearchView()
    fun setupEmptyList()

    fun setObservers() {
        observeList()
        observeNetworkError()
        observeSwipe()
        observeToast()
    }
    fun observeNetworkError()
    fun observeSwipe()
    fun observeList()
    fun observeToast()
    fun onEmptyList(isEmpty: Boolean)

}