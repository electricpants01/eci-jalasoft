package eci.technician.helpers.versionManager

object CompareVersion {

    private fun ordinalIndexOf(str: String, substring: String, n: Int): Int {
        var occurrence = n
        var pos = str.indexOf(substring)
        while (--occurrence > 0 && pos != -1) pos = str.indexOf(substring, pos + 1)
        return pos
    }

    private fun cutStringVersions(version : String):String{
        val index = ordinalIndexOf(version,".",3)
        if(index > 0)
            return version.substring(0,index)
        return version
    }
    
    fun compareVersions(v1: String, v2: String): Int {
        var versionOne = cutStringVersions(v1)
        var versionTwo = cutStringVersions(v2)

        //This code is from: https://www.geeksforgeeks.org/compare-two-version-numbers/
        var vnum1 = 0
        var vnum2 = 0
        var i = 0
        var j = 0

        while (i < versionOne.length || j < versionTwo.length) {
            while (i < versionOne.length && versionOne[i] != '.') {
                vnum1 = (vnum1 * 10 + (versionOne[i] - '0'))
                i++
            }
            while (j < versionTwo.length && versionTwo[j] != '.') {
                vnum2 = (vnum2 * 10 + (versionTwo[j] - '0'))
                j++
            }
            if (vnum1 > vnum2) return 1
            if (vnum2 > vnum1) return -1

            vnum2 = 0
            vnum1 = vnum2
            i++
            j++
        }
        return 0
    }
}