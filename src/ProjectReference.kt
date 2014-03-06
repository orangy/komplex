package komplex

import java.util.ArrayList

data class ProjectReference(val name: String)

fun ProjectReferences(name : String) = ProjectReferences().let { it.add(ProjectReference(name)); it }
fun ProjectReferences(reference : ProjectReference) = ProjectReferences().let { it.add(reference); it }
class ProjectReferences() : MutableList<ProjectReference> by ArrayList<ProjectReference>()


