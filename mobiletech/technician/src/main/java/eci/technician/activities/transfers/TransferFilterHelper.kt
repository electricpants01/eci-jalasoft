package eci.technician.activities.transfers

import eci.technician.helpers.AppAuth
import eci.technician.models.ProcessingResult
import eci.technician.models.transfers.Warehouse
import eci.technician.tools.Settings

object TransferFilterHelper {

    fun filterWarehouses(list: List<Warehouse>, viewModel: TransferViewModel): List<Warehouse> {
        val techWarehouseId =
            AppAuth.getInstance().technicianUser.warehouseId ?: return listOf<Warehouse>()
        list.forEach { warehouse ->
            if (warehouse.warehouseID == techWarehouseId) warehouse.isTechnicianWarehouse = true
        }
        return if (viewModel.transferType == Warehouse.CUSTOMER_TYPE)
            filterWarehousesInCustomerTransfer(list, viewModel)
        else
            filterWarehousesInCompanyTransfer(list, viewModel)
    }

    fun retrieveCompanyWarehouseList(processingResult: ProcessingResult?): List<Warehouse> {
        val techWarehouseId = AppAuth.getInstance().technicianUser.warehouseId
        var companyList = listOf<Warehouse>()
        processingResult?.result?.let {
            companyList = Settings.createGson()
                .fromJson(
                    it, Array<Warehouse>::class.java
                ).toList().filter { warehouse ->
                    warehouse.warehouseTypeID == Warehouse.COMPANY_TYPE ||
                            warehouse.warehouseID == techWarehouseId
                }
        }
        return companyList
    }

    private fun filterWarehousesInCompanyTransfer(
        warehouseList: List<Warehouse>,
        viewModel: TransferViewModel
    ): List<Warehouse> {
        val techWarehouseId = AppAuth.getInstance().technicianUser.warehouseId
        var filterList = warehouseList
        if (viewModel.selectingSource) {
            filterList = filterList.filter { warehouse ->
                warehouse.warehouseTypeID == Warehouse.COMPANY_TYPE
                        || warehouse.warehouseTypeID == Warehouse.TECH_TYPE || warehouse.warehouseID == techWarehouseId
            }
        } else {
            if (viewModel.sourceSelectWarehouse.value == null) {
                filterList = filterList.filter { warehouse ->
                    warehouse.warehouseTypeID == Warehouse.COMPANY_TYPE
                            || warehouse.warehouseTypeID == Warehouse.TECH_TYPE || warehouse.warehouseID == techWarehouseId
                }
            } else {
                filterList =
                    if (viewModel?.sourceSelectWarehouse?.value?.warehouseTypeID == Warehouse.COMPANY_TYPE &&
                        viewModel?.sourceSelectWarehouse?.value?.warehouseID != techWarehouseId
                    ) {
                        filterList.filter { warehouse ->
                            warehouse.warehouseTypeID == Warehouse.TECH_TYPE ||
                                    warehouse.warehouseID == techWarehouseId

                        }
                    } else {
                        filterList.filter { warehouse ->
                            warehouse.warehouseTypeID == Warehouse.COMPANY_TYPE &&
                                    warehouse.warehouseID != techWarehouseId
                        }
                    }
            }
        }
        return filterList
    }

    private fun filterWarehousesInCustomerTransfer(
        warehouseList: List<Warehouse>,
        viewModel: TransferViewModel
    ): List<Warehouse> {
        var filterList = warehouseList
        val techWarehouseId = AppAuth.getInstance().technicianUser.warehouseId
        filterList = filterRelatedWarehouse(filterList, viewModel.customerWarehouseId)
        if (viewModel.selectingSource) {
            filterList = filterWarehouseListByMultipleCriteria(
                filterList,
                WarehouseType.CUSTOMER_TYPE,
                techWarehouseId
            )
        } else {
            filterList = if (viewModel.sourceSelectWarehouse.value == null) {
                filterWarehouseListByMultipleCriteria(
                    filterList,
                    WarehouseType.CUSTOMER_TYPE,
                    techWarehouseId
                )
            } else {
                when (viewModel?.sourceSelectWarehouse?.value?.warehouseTypeID) {
                    WarehouseType.CUSTOMER_TYPE.value ->
                        filterTechWarehouse(filterList, techWarehouseId)
                    else ->
                        filterWarehouseListBySingleCriteria(filterList, WarehouseType.CUSTOMER_TYPE)
                }
            }
        }
        return filterList
    }

    private fun filterRelatedWarehouse(
        warehouseList: List<Warehouse>,
        customerWarehouseId: Int
    ): List<Warehouse> {
        val techWarehouseId = AppAuth.getInstance().technicianUser.warehouseId
        return warehouseList.filter { warehouse ->
            warehouse.warehouseID == customerWarehouseId || warehouse.warehouseID == techWarehouseId
        }
    }

    fun filterWarehouseListBySingleCriteria(
        warehouseList: List<Warehouse>,
        criteria: WarehouseType
    ): List<Warehouse> {
        return warehouseList.filter { warehouse ->
            warehouse.warehouseTypeID == criteria.value
        }
    }

    fun filterTechWarehouse(
        warehouseList: List<Warehouse>,
        warehouseId: Int
    ): List<Warehouse> {
        return warehouseList.filter { warehouse ->
            warehouse.warehouseID == warehouseId
        }
    }


    private fun filterWarehouseListByMultipleCriteria(
        warehouseList: List<Warehouse>,
        criteria1: WarehouseType,
        criteria2: Int
    ): List<Warehouse> {
        return warehouseList.filter { warehouse ->
            warehouse.warehouseTypeID == criteria1.value || warehouse.warehouseID == criteria2
        }
    }

    fun filterTechWarehouse(warehouseList: List<Warehouse>?): Warehouse? {
        val warehouseId = AppAuth.getInstance().technicianUser.warehouseId
        warehouseList?.forEach { warehouse ->
            if (warehouse.warehouseID == warehouseId) {
                warehouse.warehouseTypeID = Warehouse.TECH_TYPE
                return warehouse
            }
        }
        return null
    }

}